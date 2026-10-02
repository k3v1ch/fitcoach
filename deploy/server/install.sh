#!/usr/bin/env bash
# Установка серверной части ФитКоуч (под root, из каталога репозитория):
#   deploy/server/install.sh base      — скрипты, systemd-юниты, git-хуки, каталоги
#   deploy/server/install.sh firewall  — ufw (только 22/80/443) и фильтр DOCKER-USER
#   deploy/server/install.sh ssh       — усиление sshd, git-shell для git, fail2ban
# Повторный запуск безопасен.
set -Eeuo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
REPO=/home/git/Iinformation-system

[[ $EUID -eq 0 ]] || { echo "install.sh: нужен root" >&2; exit 1; }

base() {
  install -d -m 0755 /opt/fitcoach /opt/fitcoach/releases /opt/fitcoach/shared /opt/fitcoach/shared/nginx /var/www/certbot
  install -d -m 0700 /opt/fitcoach/shared/tls /var/lib/fitcoach /var/lib/fitcoach-ci /var/lib/fitcoach-ci/work /var/backups/fitcoach
  install -d -m 0730 -o root -g git /var/spool/fitcoach-ci
  install -d -m 0750 -o root -g git /var/log/fitcoach-ci
  local allow=/opt/fitcoach/shared/nginx/ratelimit-allow.conf
  [[ -f $allow ]] || printf '%s\n' '# IP без ограничения частоты запросов (например, для нагрузочного теста), формат nginx geo:' '# 203.0.113.10 1;' > "$allow"
  install -m 0755 "$HERE"/bin/fitcoach-* /usr/local/sbin/
  install -m 0644 "$HERE"/systemd/fitcoach-* /etc/systemd/system/
  install -m 0755 -o root -g git "$HERE"/hooks/post-receive "$HERE"/hooks/update "$REPO/.git/hooks/"
  install -d -m 0755 -o root -g git /home/git/git-shell-commands
  install -m 0755 -o root -g git "$HERE"/git-shell-commands/* /home/git/git-shell-commands/
  install -D -m 0755 "$HERE"/letsencrypt/fitcoach-reload-edge.sh /etc/letsencrypt/renewal-hooks/deploy/fitcoach-reload-edge.sh
  systemctl daemon-reload
  systemctl enable --now fitcoach-ci.path fitcoach-backup.timer
  echo "base: установлено"
}

firewall() {
  ufw allow 22/tcp >/dev/null
  ufw allow 80/tcp >/dev/null
  ufw allow 443/tcp >/dev/null
  local rule
  for rule in 2222/tcp 8443/tcp 9999/tcp 8080/tcp 443; do
    ufw --force delete allow "$rule" >/dev/null 2>&1 || true
  done
  ufw --force enable >/dev/null
  systemctl enable fitcoach-docker-fw.service >/dev/null 2>&1
  systemctl restart fitcoach-docker-fw.service
  ufw status
  iptables -S DOCKER-USER
}

ssh_harden() {
  [[ -s /root/.ssh/authorized_keys ]] || { echo "ssh: у root нет ключей — вход по паролю не отключаю" >&2; exit 1; }
  install -m 0644 "$HERE"/sshd/00-fitcoach-hardening.conf /etc/ssh/sshd_config.d/
  if ! sshd -t; then
    rm -f /etc/ssh/sshd_config.d/00-fitcoach-hardening.conf
    echo "ssh: sshd -t не прошёл, изменения откатаны" >&2
    exit 1
  fi
  systemctl reload-or-restart ssh
  grep -qx /usr/bin/git-shell /etc/shells || echo /usr/bin/git-shell >> /etc/shells
  usermod -s /usr/bin/git-shell git
  install -m 0644 "$HERE"/fail2ban/fitcoach-sshd.local /etc/fail2ban/jail.d/
  fail2ban-client reload >/dev/null
  echo "ssh: готово"
}

case "${1:-}" in
  base) base ;;
  firewall) firewall ;;
  ssh) ssh_harden ;;
  *) echo "usage: install.sh base|firewall|ssh" >&2; exit 2 ;;
esac
