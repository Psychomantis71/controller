import axios from 'axios';
import router from '../router';
import EventBus from '../event-bus';
import mySettingsObject from 'my-app-settings';
let backendApiUrl = mySettingsObject.BACKEND_API_URL;
const actions = {
  userSignIn({ commit }, payload) {
    const data = {
      username: payload.username,
      password: payload.password,
      otpCode: payload.otpCode,
    };
    commit('setLoading', true);
    axios.post(`${backendApiUrl}/login`, data)
      .then(() => {
        commit('setAuth', true);
        commit('setLoading', false);
        commit('setError', null);
        EventBus.$emit('authenticated', 'User authenticated');
        router.push('home');
      })
      .catch((error) => {
        commit('setError', error.message);
        commit('setLoading', false);
      });
  },
  userSignOut({ commit }) {
    commit('clearAuth');
    EventBus.$emit('authenticated', 'User not authenticated');
    router.push('signIn');
  },
};

export default actions;
