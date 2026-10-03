import { createRouter, createWebHistory } from 'vue-router'
import { ensureSession, currentUser, clearSession, currentOrganization, hasRole, homePath } from '../utils/session'
import { setUnauthenticatedHandler } from '../api/index'

// Auth
import AuthView from '../views/auth/AuthView.vue'
import RegisterConfirmView from '../views/auth/RegisterConfirmView.vue'
import ResetPasswordView from '../views/auth/ResetPasswordView.vue'
import OnboardingView from '../views/auth/OnboardingView.vue'

// Trainer
import DashboardView from '../views/trainer/DashboardView.vue'
import SectionsView from '../views/trainer/SectionsView.vue'
import GroupsView from '../views/trainer/GroupsView.vue'
import AthletesView from '../views/trainer/AthletesView.vue'
import AthleteCardView from '../views/trainer/AthleteCardView.vue'
import ScheduleView from '../views/trainer/ScheduleView.vue'
import TrainingsView from '../views/trainer/TrainingsView.vue'
import AttendanceView from '../views/trainer/AttendanceView.vue'
import ProgressView from '../views/trainer/ProgressView.vue'
import EventsView from '../views/trainer/EventsView.vue'
import AnnouncementsView from '../views/trainer/AnnouncementsView.vue'
import ReportsView from '../views/trainer/ReportsView.vue'
import GroupCardView from '../views/trainer/GroupCardView.vue'
import TrainingReportView from '../views/trainer/TrainingReportView.vue'
import ManagementUsersView from '../views/trainer/ManagementUsersView.vue'
import ManagementDirectoriesView from '../views/trainer/ManagementDirectoriesView.vue'
import ProfileView from '../views/trainer/ProfileView.vue'

// Trainer Finance
import FinanceDashboardView from '../views/trainer/finance/FinanceDashboardView.vue'
import FinanceChargesView from '../views/trainer/finance/FinanceChargesView.vue'
import FinanceRegistriesView from '../views/trainer/finance/FinanceRegistriesView.vue'
import FinancePaymentsView from '../views/trainer/finance/FinancePaymentsView.vue'
import FinanceTargetsView from '../views/trainer/finance/FinanceTargetsView.vue'
import FinanceExpensesView from '../views/trainer/finance/FinanceExpensesView.vue'

// Parent
import ParentDashboardView from '../views/parent/ParentDashboardView.vue'
import ParentScheduleView from '../views/parent/ParentScheduleView.vue'
import ParentAttendanceView from '../views/parent/ParentAttendanceView.vue'
import ParentProgressView from '../views/parent/ParentProgressView.vue'
import ParentCampsView from '../views/parent/ParentCampsView.vue'
import ParentAnnouncementsView from '../views/parent/ParentAnnouncementsView.vue'
import ParentPaymentsView from '../views/parent/ParentPaymentsView.vue'
import ParentContactView from '../views/parent/ParentContactView.vue'
import ParentProfileView from '../views/parent/ParentProfileView.vue'

// Athlete
import AthleteDashboardView from '../views/athlete/AthleteDashboardView.vue'
import AthleteScheduleView from '../views/athlete/AthleteScheduleView.vue'
import AthleteTrainingDetailView from '../views/athlete/AthleteTrainingDetailView.vue'
import AthleteTasksView from '../views/athlete/AthleteTasksView.vue'
import AthleteEventsView from '../views/athlete/AthleteEventsView.vue'
import AthleteProfileView from '../views/athlete/AthleteProfileView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'auth', component: AuthView, meta: { public: true } },
    // Ссылки из писем бэкенда: PUBLIC_BASE_URL + /activate?token=… и /reset-password?token=…
    { path: '/activate', name: 'activate', component: RegisterConfirmView, meta: { public: true } },
    { path: '/auth/register/confirm', name: 'register-confirm', component: RegisterConfirmView, meta: { public: true } },
    { path: '/reset-password', name: 'reset-password', component: ResetPasswordView, meta: { public: true } },
    // Пользователь без организации: создать свою (станет тренером) или ждать приглашения
    { path: '/onboarding', name: 'onboarding', component: OnboardingView },

    // ================= ТРЕНЕР =================
    {
      path: '/trainer',
      name: 'trainer',
      redirect: '/trainer/dashboard',
      children: [
        { path: 'dashboard', name: 'trainer-dashboard', component: DashboardView },
        { path: 'sections', name: 'trainer-sections', component: SectionsView },
        { path: 'groups', name: 'trainer-groups', component: GroupsView },
        { path: 'groups/:id', name: 'trainer-group-card', component: GroupCardView },
        { path: 'athletes', name: 'trainer-athletes', component: AthletesView },
        { path: 'athletes/:id', name: 'trainer-athlete-card', component: AthleteCardView },
        { path: 'schedule', name: 'trainer-schedule', component: ScheduleView },
        { path: 'trainings', name: 'trainer-trainings', component: TrainingsView },
        { path: 'trainings/:id/report', name: 'trainer-training-report', component: TrainingReportView },
        { path: 'attendance', name: 'trainer-attendance', component: AttendanceView },
        { path: 'progress', name: 'trainer-progress', component: ProgressView },
        { path: 'events', name: 'trainer-events', component: EventsView },
        { path: 'announcements', name: 'trainer-announcements', component: AnnouncementsView },
        { path: 'reports', name: 'trainer-reports', component: ReportsView },
        { path: 'management/users', name: 'trainer-management-users', component: ManagementUsersView },
        { path: 'management/directories', name: 'trainer-management-directories', component: ManagementDirectoriesView },
        { path: 'finance', name: 'trainer-finance', component: FinanceDashboardView },
        { path: 'finance/charges', name: 'trainer-finance-charges', component: FinanceChargesView },
        { path: 'finance/registries', name: 'trainer-finance-registries', component: FinanceRegistriesView },
        { path: 'finance/payments', name: 'trainer-finance-payments', component: FinancePaymentsView },
        { path: 'finance/targets', name: 'trainer-finance-targets', component: FinanceTargetsView },
        { path: 'finance/expenses', name: 'trainer-finance-expenses', component: FinanceExpensesView },
        { path: 'profile', name: 'trainer-profile', component: ProfileView },
      ]
    },

    // ================= РОДИТЕЛЬ =================
    {
      path: '/parent',
      name: 'parent',
      redirect: '/parent/dashboard',
      children: [
        { path: 'dashboard', name: 'parent-dashboard', component: ParentDashboardView },
        { path: 'schedule', name: 'parent-schedule', component: ParentScheduleView },
        { path: 'attendance', name: 'parent-attendance', component: ParentAttendanceView },
        { path: 'progress', name: 'parent-progress', component: ParentProgressView },
        { path: 'camps', name: 'parent-camps', component: ParentCampsView },
        { path: 'announcements', name: 'parent-announcements', component: ParentAnnouncementsView },
        { path: 'payments', name: 'parent-payments', component: ParentPaymentsView },
        { path: 'contact', name: 'parent-contact', component: ParentContactView },
        { path: 'profile', name: 'parent-profile', component: ParentProfileView },
      ]
    },

    // ================= СПОРТСМЕН =================
    {
      path: '/athlete',
      name: 'athlete',
      redirect: '/athlete/dashboard',
      children: [
        { path: 'dashboard', name: 'athlete-dashboard', component: AthleteDashboardView },
        { path: 'schedule', name: 'athlete-schedule', component: AthleteScheduleView },
        { path: 'schedule/:id', name: 'athlete-training-detail', component: AthleteTrainingDetailView },
        { path: 'tasks', name: 'athlete-tasks', component: AthleteTasksView },
        { path: 'events', name: 'athlete-events', component: AthleteEventsView },
        { path: 'profile', name: 'athlete-profile', component: AthleteProfileView },
      ]
    },

    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// Разделы кабинета и роли, которым они доступны
const SECTION_ROLES = {
  trainer: ['TRAINER', 'AGENCY'],
  parent: ['PARENT'],
  athlete: ['ATHLETE']
}

// Сессия на сервере закончилась — на вход, затем обратно на ту же страницу
setUnauthenticatedHandler(() => {
  if (!currentUser.value) return // первая загрузка: перенаправит beforeEach
  clearSession()
  const here = router.currentRoute.value
  if (!here.meta.public) router.push({ path: '/', query: { next: here.fullPath, expired: '1' } })
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  if (!(await ensureSession())) {
    return { path: '/', query: { next: to.fullPath } }
  }
  if (!currentOrganization.value) {
    return to.path === '/onboarding' ? true : '/onboarding'
  }
  const allowed = SECTION_ROLES[to.path.split('/')[1]]
  if (allowed && !allowed.some(hasRole)) return homePath()
  return true
})

export default router