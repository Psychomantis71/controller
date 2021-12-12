<template>
  <v-app>
    <v-app-bar
      app
      color="teal lighten-1"
      dark
    >
      <v-toolbar-title>
        <router-link
          to="/home"
          tag="span"
          style="cursor: pointer"
        >
          OPEN SOURCE CERTIFICATE MANAGER
        </router-link>
      </v-toolbar-title>
      <v-spacer />


      <v-toolbar-items class="hidden-xs-only">
        <v-btn
          v-for="item in menuItems"
          :key="item.title"
          text
          :to="item.path"
        >
          <v-icon left>
            {{ item.icon }}
          </v-icon>
          {{ item.title }}
        </v-btn>
        <v-btn
          v-if="isAuthenticated"
          text
          @click="userSignOut"
        >
          <v-icon left>
            exit_to_app
          </v-icon>
          Sign Out
        </v-btn>
      </v-toolbar-items>
    </v-app-bar>




    <v-navigation-drawer
      v-if="isAuthenticated"
      expand-on-hover
      app
    >
      <v-list>
        <v-list-item class="px-2">
          <v-list-item-avatar>
            <v-img src="../public/certificate-flat.png"></v-img>
          </v-list-item-avatar>
        </v-list-item>


      </v-list>

      <v-divider></v-divider>

      <v-list
        nav
        dense
      >
        <v-list-item
          link
          v-for="item in sideMenuItems"
          :key="item.title"
          text
          :to="item.path"
        >
          <v-list-item-icon>
            <v-icon>
              {{ item.icon }}
            </v-icon>
          </v-list-item-icon>
          <v-list-item-title>
            {{ item.title }}
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>



    <div>
      <v-content>
        <router-view />
      </v-content>
    </div>


  </v-app>
</template>

<script>
import EventBus from './event-bus';

export default {
  name: 'App',
  data() {
    return {
      isAuthenticated: false,
    };
  },
  computed: {
    menuItems() {
      if (this.isAuthenticated) {
        return [
          { title: 'Home', path: '/home', icon: 'home' },
          { title: 'User data', path: '/secured', icon: 'vpn_key' },
        ];
      }
      return [
        { title: 'Home', path: '/home', icon: 'home' },
        { title: 'Sign In', path: '/signIn', icon: 'lock_open' },
      ];
    },
    sideMenuItems() {
      if (this.isAuthenticated) {
        return [
          { title: 'Instances', path: '/instances', icon: 'desktop_windows' },
          { title: 'Keystores', path: '/keystores', icon: 'vpn_key' },
          { title: 'Certificates', path: '/certificates', icon: 'badge' },
          { title: 'Admin settings', path: '/home', icon: 'admin_panel_settings' },
          { title: 'Ca vault', path: '/CaVault', icon: 'assured_workload' },
          { title: 'Test', path: '/Test', icon: 'admin_panel_settings' },
          { title: 'Upload to agent', path: '/uploadtoagent', icon: 'upload' },
          { title: 'Users', path: '/users', icon: 'supervisor_account' },
        ];
      }
      return [];
    },
  },
  created() {
    this.isAuthenticated = localStorage.getItem('auth');
    // Use localstorage because isAuthenticated from $store is undefined when event is called
    EventBus.$on('authenticated', () => {
      this.isAuthenticated = localStorage.getItem('auth');
    });
  },
  beforeDestroy() {
    EventBus.$off('authenticated');
  },
  methods: {
    userSignOut() {
      this.$store.dispatch('userSignOut');
    },
  },
};
</script>
