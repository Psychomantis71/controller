<template>
  <v-container>
    <v-layout row>
      <v-flex
        xs12
        class="text-center"
        mt-5
      >
        <h1>Users</h1>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="getInstanceData"
        >
          Force refresh
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="adoptSelected"
        >
          Add
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
        >
          Delete
        </v-btn>
        <v-card>
          <v-card-title>
            <v-text-field
              v-model="search"
              append-icon="mdi-magnify"
              label="Search"
              single-line
              hide-details
            ></v-text-field>
          </v-card-title>
          <v-data-table
            v-model="selected"
            :headers="headers"
            :items="users"
            :search="search"
            item-key="id"
            show-select
            class="elevation-1"
          >
          </v-data-table>
        </v-card>
      </v-flex>
    </v-layout>
  </v-container>
</template>

<script>
export default {
  data() {
    return {
      selected: [],
      users: [],
      responseObj: {
        url: '',
        statusCode: '',
        method: '',
        msg: '',
        xsrfToken: '',
      },
      search: '',
      headers: [
        {
          text: 'ID',
          align: 'start',
          value: 'id',
        },
        { text: 'Username', value: 'username' },
        { text: 'Role', value: 'userRole' },
        { text: 'Email', value: 'email' },
      ],
    };
  },
  created() {
  },
  mounted() {
    this.getUserData();
  },
  methods: {
    getUserData() {
      this.$axios
        .get('http://localhost:8091/api/user/all-gui')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.users = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.users = error;
        });
    },
    deleteUser(){
      this.$axios
        .post('http://localhost:8091/api/user/delete', this.selected)
        .then((response) => {
          console.log('Post response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log(error);
        });
    },
    addUser(){
      this.$axios
        .post('http://localhost:8091/api/user/add', this.selected)
        .then((response) => {
          console.log('Post response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log(error);
        });
    },
    getColor(adopted) {
      if (adopted === false) return 'red';
      return 'green';
    },
    getStatusColor(status) {
      if (status === 'OK') return 'green';
      if (status === 'REQUIRES ATTENTION') return 'orange';
      return 'red';
    },
  },
};
</script>
