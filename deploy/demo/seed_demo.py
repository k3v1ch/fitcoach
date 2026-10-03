#!/usr/bin/env python3
"""Демо-данные ФитКоуч через публичный API (ТЗ 7.1: «подготовлены демонстрационные данные»).

Создаёт НОВУЮ организацию и наполняет её: справочники, 2 секции, 3 группы, 18 спортсменов, тренировки
за 3 прошедшие недели (посещаемость, закрытые отчёты) и расписание на 2 недели вперёд (одна тренировка
перенесена, одна отменена), результаты, мероприятия (сборы, соревнования, целевой сбор, медосмотр),
абонементы и оплаты (в том числе долги), объявления, медицинские справки. Существующие организации и
записи не меняются.

Аккаунты регистрируются обычным способом на сайте (письмо с подтверждением):
  * тренер — обязательно: от его имени создаётся организация;
  * родитель и спортсмен — по желанию: их добавят в организацию по email и привяжут к карточкам
    (спортсмен — к своей карточке, родитель — к ней и к карточке младшего ребёнка).

  DEMO_TRAINER_PASSWORD='…' python3 deploy/demo/seed_demo.py \\
      --trainer-email trainer@example.org --parent-email parent@example.org --athlete-email athlete@example.org

Только стандартная библиотека Python 3.8+. Скорость ограничена (лимит nginx 20 запросов/с).
"""
import argparse
import datetime as dt
import getpass
import http.cookiejar
import json
import os
import random
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

MSK = dt.timezone(dt.timedelta(hours=3))  # демо-организация — Europe/Moscow (без перехода на летнее время)


class Api:
    def __init__(self, base, cafile=None, pause=0.06):
        self.base = base.rstrip("/") + "/api/v1"
        handlers = [urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())]
        if cafile:  # тестовый стенд с собственным сертификатом: доверяем именно ему, проверка TLS остаётся
            handlers.append(urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=cafile)))
        self.opener = urllib.request.build_opener(*handlers)
        self.csrf = None
        self.pause = pause
        self.count = 0

    def call(self, method, path, body=None, headers=None, raw=None, ctype="application/json"):
        if method != "GET" and self.csrf is None:
            self.csrf = json.loads(self.opener.open(self.base + "/auth/csrf").read())["token"]
        h = dict(headers or {})
        if body is not None or raw is not None:
            h["Content-Type"] = ctype
        if method != "GET":
            h["X-CSRF-TOKEN"] = self.csrf
        data = raw if raw is not None else (None if body is None else json.dumps(body).encode())
        for attempt in range(6):
            time.sleep(self.pause)
            self.count += 1
            try:
                r = self.opener.open(urllib.request.Request(self.base + path, method=method, data=data, headers=h))
                payload = r.read()
                return json.loads(payload) if payload and "json" in (r.headers.get("Content-Type") or "") else None
            except urllib.error.HTTPError as e:
                payload = e.read()
                if e.code in (429, 502, 503) and attempt < 5:
                    time.sleep(2 * (attempt + 1))
                    continue
                try:
                    err = json.loads(payload)
                except Exception:
                    err = {"message": payload[:200].decode(errors="replace")}
                fields = "; ".join(f"{f.get('field')}: {f.get('message')}" for f in err.get("fieldErrors") or [])
                raise SystemExit(f"{method} {path} → {e.code} {err.get('message')} {fields}".strip())

    def all(self, path, params=None):
        items, page = [], 0
        while True:
            q = dict(params or {}, page=page, size=100)
            res = self.call("GET", f"{path}?{urllib.parse.urlencode(q)}")
            items += res["items"]
            page += 1
            if page >= res["totalPages"]:
                return items


def iso(moment):
    return moment.isoformat()


def at(day, hour, minute=0):
    return dt.datetime(day.year, day.month, day.day, hour, minute, tzinfo=MSK)


def split_name(full, fallback_last):
    parts = (full or "").split()
    if len(parts) >= 2:
        return parts[0], parts[1]
    return (parts[0] if parts else "Демо"), fallback_last


def pdf(title):
    text = title.encode("cp1251", errors="replace")
    return b"%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Count 0/Kids[]>>endobj\n" \
           b"% " + text + b"\ntrailer<</Root 1 0 R>>\n%%EOF\n"


def multipart(fields, filename, content):
    boundary = "----fitcoach" + uuid.uuid4().hex
    out = []
    for k, v in fields.items():
        out.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
    out.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\n'
               f"Content-Type: application/pdf\r\n\r\n".encode() + content + b"\r\n")
    out.append(f"--{boundary}--\r\n".encode())
    return b"".join(out), f"multipart/form-data; boundary={boundary}"


SWIM_OLDER = [("Илья", "Смирнов"), ("Дарья", "Волкова"), ("Максим", "Орлов"), ("Полина", "Зайцева"),
              ("Артём", "Белов"), ("Ксения", "Морозова"), ("Егор", "Лебедев")]
SWIM_YOUNGER = [("Анна", "Смирнова"), ("Тимофей", "Ковалёв"), ("Варвара", "Соловьёва"), ("Матвей", "Фёдоров"),
                ("Алиса", "Павлова")]
SPRINT = [("Никита", "Киселёв"), ("София", "Андреева"), ("Глеб", "Макаров"), ("Ева", "Николаева"),
          ("Даниил", "Захаров"), ("Мирослава", "Ершова")]

TOPICS = {
    "swim": [("Техника кроля: работа ног", "Разминка 400 м; 8×50 м ноги с доской; 6×100 м кроль с акцентом на ноги; заминка 200 м."),
             ("Старты и повороты", "Разминка 300 м; 10 стартов с тумбы; 12 поворотов «сальто»; 4×25 м максимально."),
             ("Аэробная выносливость", "Разминка 400 м; 3×400 м кроль на пульсе 150; 8×50 м комплекс; заминка 200 м."),
             ("Спина: техника гребка", "Разминка 300 м; 8×50 м на спине с акцентом на гребок; 4×100 м на спине; заминка.")],
    "kids": [("Обучение скольжению", "Разминка на суше; скольжение на груди и спине; упражнения с доской; игры в воде."),
             ("Дыхание и ноги кролем", "Разминка; выдохи в воду 3×10; ноги кролем с доской 6×12,5 м; игра «Поплавок»."),
             ("ОФП для пловцов", "Суставная разминка; упражнения на гибкость; планка; игры на координацию.")],
    "sprint": [("Стартовый разгон", "Разминка 15 мин; беговые упражнения; 6×30 м с низкого старта; заминка."),
               ("Скоростная выносливость", "Разминка; 4×150 м через 3 мин; растяжка."),
               ("Техника бега", "СБУ: высокое поднимание бедра, захлёст, многоскоки; 5×60 м по технике.")],
}


def main():
    ap = argparse.ArgumentParser(description="Демо-данные ФитКоуч через API")
    ap.add_argument("--base", default="https://fitcoach.keldari.online")
    ap.add_argument("--trainer-email", required=True)
    ap.add_argument("--parent-email")
    ap.add_argument("--athlete-email")
    ap.add_argument("--org-name", default="СШ «Олимп» (демо)")
    ap.add_argument("--cafile", help="сертификат тестового стенда (PEM), которому доверять")
    args = ap.parse_args()
    password = os.environ.get("DEMO_TRAINER_PASSWORD") or getpass.getpass("Пароль тренера: ")

    rnd = random.Random(2026)
    api = Api(args.base, cafile=args.cafile)
    api.call("POST", "/auth/login", {"email": args.trainer_email, "password": password})
    api.csrf = None  # после входа — новая сессия и новый CSRF-токен
    me = api.call("GET", "/me")
    today = dt.datetime.now(MSK).date()
    print(f"Вход: {me['fullName'] or me['email']}; организация «{args.org_name}»")

    org = api.call("POST", "/organizations", {
        "name": args.org_name, "description": "Демонстрационная спортивная школа: плавание и лёгкая атлетика.",
        "address": "г. Москва, ул. Спортивная, 12", "timezone": "Europe/Moscow"})
    O = f"/organizations/{org['id']}"

    # --- участники по email ---
    parent = api.call("POST", f"{O}/members/parents", {"email": args.parent_email}) if args.parent_email else None
    athlete_member = api.call("POST", f"{O}/members/athletes", {"email": args.athlete_email}) if args.athlete_email else None

    # --- справочники ---
    def dictionary(kind, name, **extra):
        return api.call("POST", f"{O}/dictionaries/{kind}", {"name": name, "sortOrder": extra.pop("order", 0), "status": "ACTIVE", **extra})
    swim_type = dictionary("sport-types", "Плавание")
    run_type = dictionary("sport-types", "Лёгкая атлетика", order=1)
    t_tech = dictionary("training-types", "Техника")
    t_ofp = dictionary("training-types", "ОФП", order=1)
    t_endurance = dictionary("training-types", "Выносливость", order=2)
    pool = dictionary("venues", "Бассейн «Волна»", address="ул. Спортивная, 12", description="25 м, 6 дорожек")
    arena = dictionary("venues", "Манеж «Олимп»", address="ул. Ленина, 5", order=1)
    gym = dictionary("venues", "Зал ОФП", address="ул. Спортивная, 12, 2 этаж", order=2)

    # --- секции и группы ---
    swim = api.call("POST", f"{O}/sections", {"name": "Плавание", "sportTypeId": swim_type["id"], "status": "ACTIVE",
                                               "description": "Спортивное плавание, дети 8–15 лет"})
    run = api.call("POST", f"{O}/sections", {"name": "Лёгкая атлетика", "sportTypeId": run_type["id"], "status": "ACTIVE",
                                              "description": "Спринт и прыжки"})
    coach = [me["userId"]]
    g_older = api.call("POST", f"{O}/groups", {"name": "Плавание · Старшие", "sectionId": swim["id"], "coachIds": coach, "status": "ACTIVE",
                                                "description": "12–15 лет, спортивное совершенствование"})
    g_younger = api.call("POST", f"{O}/groups", {"name": "Плавание · Младшие", "sectionId": swim["id"], "coachIds": coach, "status": "ACTIVE",
                                                  "description": "8–10 лет, начальная подготовка"})
    g_sprint = api.call("POST", f"{O}/groups", {"name": "Лёгкая атлетика · Спринт", "sectionId": run["id"], "coachIds": coach, "status": "ACTIVE",
                                                 "description": "11–14 лет"})

    # --- спортсмены ---
    if athlete_member:
        SWIM_OLDER[0] = split_name(athlete_member["fullName"], "Смирнов")
    joined = today - dt.timedelta(days=75)
    roster = {}

    def add_athletes(names, group, years, note=None):
        cards = []
        for i, (first, last) in enumerate(names):
            birth = dt.date(today.year - years[0] - (i % (years[1] - years[0] + 1)), 1 + (i * 5) % 12, 1 + (i * 7) % 27)
            card = api.call("POST", f"{O}/athletes", {"firstName": first, "lastName": last, "birthDate": birth.isoformat(),
                                                      "status": "ACTIVE", "enrolledOn": joined.isoformat(), "note": note})
            api.call("POST", f"{O}/groups/{group['id']}/athletes", {"athleteId": card["id"], "joinedOn": joined.isoformat()})
            cards.append(card)
        roster[group["id"]] = cards
        return cards

    older = add_athletes(SWIM_OLDER, g_older, (12, 15))
    younger = add_athletes(SWIM_YOUNGER, g_younger, (8, 10))
    sprint = add_athletes(SPRINT, g_sprint, (11, 14))
    star, sibling = older[0], younger[0]
    if athlete_member:
        api.call("PATCH", f"{O}/athletes/{star['id']}", {"userId": athlete_member["userId"]})
    if parent:
        link = [{"parentUserId": parent["userId"], "relationship": "мать"}]
        api.call("PATCH", f"{O}/athletes/{star['id']}", {"parentLinks": link})
        api.call("PATCH", f"{O}/athletes/{sibling['id']}", {"parentLinks": link})

    # --- тренировки: 3 недели назад и 2 недели вперёд ---
    plan = {"swim": [{"title": "Разминка", "durationMinutes": 15, "description": None},
                     {"title": "Основная часть", "durationMinutes": 60, "description": None},
                     {"title": "Заминка", "durationMinutes": 15, "description": None}],
            "kids": [{"title": "Разминка на суше", "durationMinutes": 10, "description": None},
                     {"title": "Обучение в воде", "durationMinutes": 40, "description": None},
                     {"title": "Игры", "durationMinutes": 10, "description": None}],
            "sprint": [{"title": "Разминка", "durationMinutes": 20, "description": None},
                       {"title": "Беговая работа", "durationMinutes": 55, "description": None},
                       {"title": "Растяжка", "durationMinutes": 15, "description": None}]}
    schedule = [  # группа, дни недели (0 = пн), час, минуты, длительность, площадка, тип, вид
        (g_older, (0, 2, 4), 17, 0, 90, pool, t_endurance, "swim"),
        (g_younger, (1, 3), 16, 0, 60, pool, t_tech, "kids"),
        (g_younger, (5,), 11, 0, 60, gym, t_ofp, "kids"),
        (g_sprint, (0, 2, 4), 18, 0, 90, arena, t_tech, "sprint"),
    ]
    past, future = [], []
    for offset in range(-21, 15):
        day = today + dt.timedelta(days=offset)
        for group, weekdays, hour, minute, minutes, venue, ttype, kind in schedule:
            if day.weekday() not in weekdays:
                continue
            start = at(day, hour, minute)
            topic = TOPICS[kind][(offset + 21) % len(TOPICS[kind])]
            training = api.call("POST", f"{O}/trainings", {
                "title": topic[0], "groupId": group["id"], "coachIds": coach, "venueId": venue["id"], "typeId": ttype["id"],
                "startsAt": iso(start), "endsAt": iso(start + dt.timedelta(minutes=minutes)), "plan": plan[kind], "comment": None})
            (past if start + dt.timedelta(minutes=minutes) < dt.datetime.now(MSK) else future).append((training, group, topic))

    latest_older = max((t for t in past if t[1] is g_older), key=lambda t: t[0]["startsAt"], default=None)
    for training, group, topic in past:
        marks = []
        for card in roster[group["id"]]:
            roll = rnd.random()
            if roll < 0.84:
                marks.append({"athleteId": card["id"], "status": "PRESENT", "reason": None, "comment": None})
            elif roll < 0.93:
                marks.append({"athleteId": card["id"], "status": "SICK", "reason": "ОРВИ, справка", "comment": None})
            else:
                marks.append({"athleteId": card["id"], "status": "ABSENT", "reason": "семейные обстоятельства", "comment": None})
        api.call("PUT", f"{O}/trainings/{training['id']}/attendance", marks)
        last = latest_older is not None and training["id"] == latest_older[0]["id"]
        api.call("PUT", f"{O}/trainings/{training['id']}/report", {
            "topic": topic[0], "actualContent": topic[1],
            "comment": "Черновик: дописать замечания по технике." if last else "Группа отработала план полностью.",
            "status": "DRAFT" if last else "CLOSED"})

    # одна будущая тренировка перенесена, одна отменена — участники получают уведомления
    moved = next((t for t in future if t[1] is g_older), None)
    if moved:
        start = dt.datetime.fromisoformat(moved[0]["startsAt"].replace("Z", "+00:00")) + dt.timedelta(hours=1)
        api.call("PATCH", f"{O}/trainings/{moved[0]['id']}", {"startsAt": iso(start), "endsAt": iso(start + dt.timedelta(minutes=90))})
    cancelled = next((t for t in future if t[1] is g_younger), None)
    if cancelled:
        api.call("PATCH", f"{O}/trainings/{cancelled[0]['id']}", {"status": "CANCELLED", "cancelReason": "Санитарная обработка бассейна"})

    # --- результаты ---
    def results(card, metric, unit, values, comment):
        best = min(values) if unit == "с" else max(values)
        for weeks_ago, value in zip((6, 4, 2, 0), values):
            measured = today - dt.timedelta(days=7 * weeks_ago + 1)
            api.call("POST", f"{O}/athletes/{card['id']}/results", {
                "metricName": metric, "value": f"{value:.2f}", "unit": unit, "measuredOn": measured.isoformat(),
                "comment": comment if weeks_ago == 0 else None, "isPersonalBest": value == best})
    for i, card in enumerate(older[:5]):
        base = 33.8 + i * 0.9
        results(card, "50 м вольный стиль", "с", [base + 1.1, base + 0.7, base + 0.4, base], "Прогресс на старте и повороте")
        results(card, "100 м комплекс", "с", [base * 2.6 + 3, base * 2.6 + 1.8, base * 2.6 + 1, base * 2.6], None)
    for i, card in enumerate(sprint[:4]):
        results(card, "Бег 60 м", "с", [9.4 - i * 0.2, 9.25 - i * 0.2, 9.1 - i * 0.2, 9.0 - i * 0.2], "Улучшить стартовый разгон")
        results(card, "Прыжок в длину с места", "м", [1.78 + i * 0.05, 1.82 + i * 0.05, 1.86 + i * 0.05, 1.9 + i * 0.05], None)

    # --- мероприятия ---
    def event(title, type_, section, start=None, days=0, location=None, cost=None, target=None, due=None,
              deadline=None, docs=(), participants=(), publish=True, description=None):
        body = {"title": title, "type": type_, "sectionId": section["id"], "description": description,
                "startsAt": iso(start) if start else None,
                "endsAt": iso(start + dt.timedelta(days=days) if days else start + dt.timedelta(hours=4)) if start else None,
                "location": location, "costPerAthlete": cost, "targetAmount": target,
                "collectionDueOn": due.isoformat() if due else None,
                "responseDeadline": iso(deadline) if deadline else None, "requiredDocumentTypes": list(docs)}
        ev = api.call("POST", f"{O}/events", body)
        if participants:
            api.call("PUT", f"{O}/events/{ev['id']}/participants", [c["id"] for c in participants])
        if publish:
            ev = api.call("PATCH", f"{O}/events/{ev['id']}", {"status": "PUBLISHED"})
        return ev
    camp = event("Осенние сборы в Сочи", "CAMP", swim, at(today + dt.timedelta(days=20), 9), days=7,
                 location="Сочи, УТЦ «Юность»", cost="35000.00", deadline=at(today + dt.timedelta(days=10), 21),
                 docs=("MEDICAL_CERTIFICATE", "CONSENT"), participants=older,
                 description="Двухразовые тренировки, ОФП, восстановление. Проживание и питание включены.")
    event("Первенство города по плаванию", "COMPETITION", swim, at(today + dt.timedelta(days=5), 10),
          location="Бассейн «Волна»", participants=older[:5] + younger[:3], deadline=at(today + dt.timedelta(days=3), 21),
          description="Дистанции 50 и 100 м. Сбор в 9:15.")
    fund = event("Сбор на стартовые тумбы", "FUNDRAISER", swim, target="60000.00", due=today + dt.timedelta(days=30),
                 participants=older + younger, description="Новые тумбы для бассейна «Волна».")
    event("Углублённый медосмотр", "MEDICAL_EXAM", run, at(today + dt.timedelta(days=9), 9),
          location="Врачебно-физкультурный диспансер № 1", participants=sprint, publish=False)

    # --- начисления и оплаты ---
    def pay(charge, amount, paid_on, method):
        api.call("POST", f"{O}/payments", {"chargeId": charge["id"], "amount": f"{amount:.2f}", "paidOn": paid_on.isoformat(),
                                           "method": method, "comment": None}, headers={"Idempotency-Key": str(uuid.uuid4())})
    month = today.replace(day=1)
    prev = (month - dt.timedelta(days=1)).replace(day=1)
    methods = ("SBP", "SBP", "CASH", "TRANSFER")
    debtors = {older[3]["id"], younger[2]["id"], sprint[4]["id"]}
    for section, cards, price in ((swim, older + younger, 4500), (run, sprint, 3500)):
        for i, card in enumerate(cards):
            for period, due_day in ((prev, 10), (month, 10)):
                period_end = (period.replace(day=28) + dt.timedelta(days=4)).replace(day=1) - dt.timedelta(days=1)
                charge = api.call("POST", f"{O}/charges", {
                    "athleteId": card["id"], "sectionId": section["id"], "type": "SUBSCRIPTION",
                    "title": f"Абонемент: {['январь','февраль','март','апрель','май','июнь','июль','август','сентябрь','октябрь','ноябрь','декабрь'][period.month - 1]}",
                    "amount": f"{price:.2f}", "dueOn": period.replace(day=due_day).isoformat(),
                    "periodFrom": period.isoformat(), "periodTo": period_end.isoformat(), "trainingId": None, "eventId": None, "comment": None})
                paid_on = min(period.replace(day=3 + i % 6), today)
                if period == prev and card["id"] not in debtors:
                    pay(charge, price, paid_on, methods[i % 4])
                elif period == month and i % 3 == 0:
                    pay(charge, price, paid_on, methods[i % 4])
                elif period == month and i % 3 == 1 and card["id"] not in debtors:
                    pay(charge, price / 2, paid_on, "CASH")
    for i, card in enumerate(older):
        charge = api.call("POST", f"{O}/charges", {
            "athleteId": card["id"], "sectionId": swim["id"], "type": "EVENT", "title": "Сборы в Сочи",
            "amount": "35000.00", "dueOn": (today + dt.timedelta(days=12)).isoformat(), "periodFrom": None, "periodTo": None,
            "trainingId": None, "eventId": camp["id"], "comment": None})
        if i % 2 == 0:
            pay(charge, 10000, today - dt.timedelta(days=i % 3), "TRANSFER")
    for i, card in enumerate((older + younger)[:8]):
        charge = api.call("POST", f"{O}/charges", {
            "athleteId": card["id"], "sectionId": swim["id"], "type": "EVENT", "title": "Сбор на стартовые тумбы",
            "amount": "3000.00", "dueOn": (today + dt.timedelta(days=30)).isoformat(), "periodFrom": None, "periodTo": None,
            "trainingId": None, "eventId": fund["id"], "comment": None})
        if i % 3 != 2:
            pay(charge, 3000, today - dt.timedelta(days=i % 4), "SBP")

    # --- объявления (получатели — аккаунты родителя и спортсмена) ---
    recipients = [m["userId"] for m in (parent, athlete_member) if m]
    if recipients:
        def announce(title, text, requires=False, deadline=None, publish=True):
            body = {"title": title, "text": text, "category": None, "recipientUserIds": recipients,
                    "requiresResponse": requires, "responseDeadline": iso(deadline) if deadline else None, "attachmentFileIds": []}
            a = api.call("POST", f"{O}/announcements", body)
            if publish:
                api.call("PATCH", f"{O}/announcements/{a['id']}?status=PUBLISHED", body)
        announce("Согласие на участие в сборах", "Подтвердите участие ребёнка в осенних сборах в Сочи. "
                 "Стоимость 35 000 ₽, предоплата 10 000 ₽ до конца недели.", True, at(today + dt.timedelta(days=7), 21))
        announce("Медицинские справки", "До конца месяца обновите медицинские справки: без действующей справки "
                 "спортсмен не допускается к соревнованиям.")
        announce("Родительское собрание", "Собрание в субботу в 12:00 в зале ОФП.", publish=False)

    # --- документы ---
    for i, card in enumerate(older[:4]):
        issued = today - dt.timedelta(days=200 if i == 3 else 30 + i * 10)
        raw, ctype = multipart({"title": "Медицинская справка", "type": "MEDICAL_CERTIFICATE", "athleteId": card["id"],
                                "issuedOn": issued.isoformat(), "validUntil": (issued + dt.timedelta(days=180)).isoformat()},
                               "spravka.pdf", pdf(f"Медицинская справка {card['lastName']}"))
        api.call("POST", f"{O}/documents", raw=raw, ctype=ctype)

    print(f"Готово: организация {org['id']}; спортсменов {len(older) + len(younger) + len(sprint)}, "
          f"тренировок {len(past)} прошедших и {len(future)} запланированных; запросов к API: {api.count}")
    if parent:
        print(f"Родитель {args.parent_email}: дети {star['firstName']} {star['lastName']} и {sibling['firstName']} {sibling['lastName']}")
    if athlete_member:
        print(f"Спортсмен {args.athlete_email}: карточка {star['firstName']} {star['lastName']}")


if __name__ == "__main__":
    main()
