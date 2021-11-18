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
          @click="getUserData"
        >
          Force refresh
        </v-btn>

        <v-dialog
          v-model="dialog"
          max-width="800px"
        >
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              dark
              color="teal lighten-1"
              class="ma-2"
              v-bind="attrs"
              v-on="on"
            >
              Add user
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Add user</span>
            </v-card-title>

            <v-card-text>
              <v-container>
                <v-row>
                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-text-field
                      v-model="userToAdd.username"
                      label="Username"
                      name="userToAdd"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-text-field
                      v-model="userToAdd.password"
                      :append-icon="show1 ? 'mdi-eye' : 'mdi-eye-off'"
                      :rules="[rules.required]"
                      :type="show1 ? 'text' : 'password'"
                      name="passwordInput"
                      label="Password"
                      hint="Select an initial password for the user"
                      value=""
                      class="input-group--focused"
                      @click:append="show1 = !show1"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="12"
                  >
                    <v-autocomplete
                      v-model="userToAdd.userRole"
                      :items="roles"
                      outlined
                      dense
                      chips
                      small-chips
                      label="User role"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="6"
                  >
                    <v-text-field
                      v-model="userToAdd.email"
                      label="User email"
                      name="userEmail"
                    />
                  </v-col>
                </v-row>
              </v-container>
            </v-card-text>

            <v-card-actions>
              <v-spacer />
              <v-btn
                color="blue darken-1"
                text
                @click="close"
              >
                Cancel
              </v-btn>
              <v-btn
                color="blue darken-1"
                text
                @click="save"
              >
                Save
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="deleteUser"
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

            <template v-slot:item.userRole="{ item }">
              <v-chip
                :color="getRoleColor(item.userRole)"
                dark
              >
                {{ item.userRole }}
              </v-chip>
            </template>

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
      dialog: false,
      show1: false,
      rules: {
        required: (value) => !!value || 'Required.',
      },
      selected: [],
      userToAdd: {
        username: '',
        password: '',
        userRole:'',
        email:''
      },
      roles: ['ADMIN','USER','RECIPIENT'],
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
        .post('http://localhost:8091/api/user/add', this.userToAdd)
        .then((response) => {
          console.log('Post response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log(error);
        });
    },
    getRoleColor(role) {
      if (role === 'ADMIN') return 'blue';
      if (role === 'USER') return 'green';
      return 'grey';
    },
    close() {
      this.dialog = false;
    },
    save() {
      console.log(this.userToAdd)
      this.addUser();
      this.close();
      this.getUserData();
    },
  },
};
</script>
