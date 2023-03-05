<template>
  <v-container>
    <v-layout row>
      <v-flex
        xs12
        class="text-center"
        mt-5
      >
        <h1>Instances</h1>
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
          Adopt
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="removeInstances"
        >
          Delete
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
              @click="getAssignedData"
            >
              Assign users to instance
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Assign users to instance</span>
            </v-card-title>

            <v-card-text>
              <v-container>
                <v-row>
                  <v-col
                    cols="12"
                    sm="6"
                    md="12"
                  >
                    <v-autocomplete
                      v-model="assignedUsers"
                      :items="allUsers"
                      outlined
                      dense
                      chips
                      small-chips
                      label="Assigned users"
                      multiple
                      item-value="id"
                      item-text="username"
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
            :items="instances"
            :search="search"
            item-key="id"
            show-select
            class="elevation-1"
          >
            <template v-slot:item.adopted="{ item }">
              <v-chip
                :color="getColor(item.adopted)"
                dark
              >
                {{ item.adopted }}
              </v-chip>
            </template>
          </v-data-table>
        </v-card>
      </v-flex>
    </v-layout>
  </v-container>
</template>

<script>
import mySettingsObject from 'my-app-settings';
let backendApiUrl = mySettingsObject.BACKEND_API_URL;
  export default {
  data() {
    return {
      selected: [],
      dialog: false,
      allUsers:[],
      assignedUsers:[],
      instances: [],
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
          text: 'Instance',
          align: 'start',
          value: 'name',
        },
        { text: 'ID', value: 'id' },
        { text: 'Hostname', value: 'hostname' },
        { text: 'IP Address', value: 'ip' },
        { text: 'Port', value: 'port' },
        { text: 'Adopted', value: 'adopted' },
      ],
    };
  },
  created() {
  },
  mounted() {
    this.getInstanceData();
  },
  methods: {
    getInstanceData() {
      this.$axios
        .get(`${backendApiUrl}/api/instance/all`)
        .then((response) => {
          console.log('Get response: ', response.data);
          this.instances = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.instances = error;
        });
    },
    removeInstances() {
      console.log(this.selected)
      this.$axios
        .post(`${backendApiUrl}/api/instance/remove`, this.selected)
        .then((response) => {
          console.log('Get response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
      this.getInstanceData()
    },
    getAllUserData() {
      this.$axios
        .get(`${backendApiUrl}/api/user/all-gui`)
        .then((response) => {
          console.log('Get response: ', response.data);
          this.allUsers = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.allUsers = error;
        });
    },
    getAssignedUserData() {
      this.$axios
        .get(`${backendApiUrl}/api/instance/${this.selected[0].id}/get-assigned`)
        .then((response) => {
          console.log('Get response: ', response.data);
          this.assignedUsers = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.assignedUsers = error;
        });
    },
    setAssignedUserData() {
      this.$axios
        .post(`${backendApiUrl}/api/instance/${this.selected[0].id}/set-assigned`, this.assignedUsers)
        .then((response) => {
          console.log('Get response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
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
    adoptSelected() {
      this.$axios
        .put(`${backendApiUrl}/api/instance/adopt`, this.selected)
        .then((response) => {
          console.log('Get response: ', response.data);
          this.getInstanceData();
        })
        .catch((error) => {
          this.alert = true;
          console.log('Error while adopting: ', error);
        });
    },
    close() {
      this.dialog = false;
    },
    save() {
      this.setAssignedUserData();
      this.close();
    },
    getAssignedData(){
      this.getAllUserData();
      this.getAssignedUserData();
    }
  },
};
</script>
