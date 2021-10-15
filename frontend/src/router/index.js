import Vue from 'vue';
import Router from 'vue-router';
import Home from '../components/Home.vue';
import NotFound from '../components/NotFound.vue';
import SignIn from '../components/SignIn.vue';
import Secured from '../components/Secured.vue';
import Instances from '../components/Instances';
import Keystores from "@/components/Keystores";
import Certificates from "@/components/Certificates";
import Test from "@/components/Test";
Vue.use(Router);

const router = new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/home',
      name: 'Home',
      component: Home,
      meta: { nonRequiresAuth: true },
    },
    {
      path: '/secured',
      name: 'Secured',
      component: Secured,
    },
    {
      path: '/instances',
      name: 'Instances',
      component: Instances,
    },
    {
      path: '/test',
      name: 'Test',
      component: Test,
    },
    {
      path: '/keystores',
      name: 'Keystores',
      component: Keystores,
    },{
      path: '/certificates',
      name: 'Certificates',
      component: Certificates,
    },
    {
      path: '/signIn',
      name: 'signIn',
      component: SignIn,
      meta: { loginPage: true, nonRequiresAuth: true },
    },
    {
      path: '*',
      component: NotFound,
    },
  ],
});

router.beforeEach((to, from, next) => {
  const requiresAuth = !to.matched.some((record) => record.meta.nonRequiresAuth);
  const isLoginPage = to.matched.some((record) => record.meta.loginPage);
  const isAuthenticated = localStorage.getItem('auth');
  if (requiresAuth && !isAuthenticated) {
    next('/signIn');
  } else if (isLoginPage && isAuthenticated) {
    router.push('/home');
  }
  next();
});

export default router;
