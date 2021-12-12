<template>
  <v-container>
    <v-layout row>
      <v-flex
        xs12
        class="text-center"
        mt-5
      >
        <h1>User data</h1>
      </v-flex>

      <v-flex
        xs8
        offset-xs2
        class="text-left"
        mt-5
      >
        <h2> User ID: {{ userData.id }}</h2>
        <h2>Username: {{ userData.username }}</h2>
        <h2>Password:

          <v-dialog
            v-model="dialogPwdChange"
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
                Change password
              </v-btn>
            </template>
            <v-card>
              <v-card-title>
                <span class="text-h5">Change password</span>
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
                        v-model="userData.password"
                        :append-icon="show1 ? 'mdi-eye' : 'mdi-eye-off'"
                        :rules="[rules.required]"
                        :type="show1 ? 'text' : 'password'"
                        name="passwordInput"
                        label="Password"
                        hint="Your new password"
                        value=""
                        class="input-group--focused"
                        @click:append="show1 = !show1"
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
                  @click="closePwdChange"
                >
                  Cancel
                </v-btn>
                <v-btn
                  color="blue darken-1"
                  text
                  @click="savePwdChange"
                >
                  Save
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>

        </h2>
        <h2>User role: {{ userData.userRole }}</h2>
        <h2>Email: {{ userData.email }}

          <v-dialog
            v-model="dialogEmailChange"
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
                Change email
              </v-btn>
            </template>
            <v-card>
              <v-card-title>
                <span class="text-h5">Change email</span>
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
                        v-model="userData.email"
                        :rules="[rules.required]"
                        label="Email"
                        hint="Your new email"
                        class="input-group--focused"
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
                  @click="closeEmailChange"
                >
                  Cancel
                </v-btn>
                <v-btn
                  color="blue darken-1"
                  text
                  @click="saveEmailChange"
                >
                  Save
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>

        </h2>

      </v-flex>
    </v-layout>
  </v-container>
</template>

<script>
export default {
  data() {
    return {
      dialogPwdChange: false,
      dialogEmailChange: false,
      rules: {
        required: (value) => !!value || 'Required.',
      },
      show1: false,
      responseObj: {
        url: '',
        statusCode: '',
        method: '',
        msg: '',
        xsrfToken: '',
      },
      userData:{
        id: '',
        username: '',
        password:'',
        userRole: '',
        email: '',
      }
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
        .get('http://localhost:8091/api/user/user-data')
        .then((response) => {
          this.userData = response.data;
        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
    },
    changePassword() {
      this.$axios
        .post(`http://localhost:8091/api/user/${this.userData.id}/change-password`, this.userData)
        .then((response) => {
          console.log(response)
          this.getUserData();
        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
    },
    changeEmail() {
      this.$axios
        .post(`http://localhost:8091/api/user/${this.userData.id}/change-email`, this.userData)
        .then((response) => {
          console.log(response)
          this.getUserData();
        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
    },
    closePwdChange(){
      this.dialogPwdChange=false;
    },
    savePwdChange(){
      this.changePassword();
      this.dialogPwdChange=false;
      this.getUserData();
    },
    closeEmailChange(){
      this.dialogEmailChange=false;
    },
    saveEmailChange(){
      this.changeEmail();
      this.dialogEmailChange=false;
      this.getUserData();
    },
    parseResponse(response) {
      const respObj = {};
      respObj.url = response.config.url;
      respObj.statusCode = response.status;
      respObj.method = response.config.method;
      respObj.msg = response.data.message ? response.data.message : response.data;
      respObj.xsrfToken = response.config.headers['X-XSRF-TOKEN'];
      return respObj;
    },
  },
};
</script>
