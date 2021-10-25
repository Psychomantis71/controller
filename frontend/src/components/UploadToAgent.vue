<template>
  <v-container>
    <v-layout row>
      <v-flex
        xs12
        class="text-center"
        mt-5
      >
        <h1>Upload to agent</h1>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="getKeystoreData"
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
              @click="getInstanceData"
            >
              Add keystore
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Add keystore</span>
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
                      v-model="editedItem.keystorepath"
                      label="Keystore path"
                      name="keystorePath"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-text-field
                      v-model="editedItem.password"
                      :append-icon="show1 ? 'mdi-eye' : 'mdi-eye-off'"
                      :rules="[rules.required]"
                      :type="show1 ? 'text' : 'password'"
                      name="passwordInput"
                      label="Password"
                      hint="If selecting multiple instances assure that the password is the same"
                      value=""
                      class="input-group--focused"
                      @click:append="show1 = !show1"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-text-field
                      v-model="editedItem.description"
                      label="Description"
                      name="descriptionFiled"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="12"
                  >
                    <v-autocomplete
                      v-model="editedItem.instance"
                      :items="agentInstances"
                      outlined
                      dense
                      chips
                      small-chips
                      label="Instances"
                      multiple
                      item-value="id"
                      item-text="hostname"
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
        >
          Edit
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
            />
          </v-card-title>
          <v-data-table
            v-model="selected"
            :headers="headers"
            :items="keystores"
            :search="search"
            item-key="id"
            show-select
            class="elevation-1"
          >
            <template v-slot:item.status="{ item }">
              <v-chip
                :color="getStatusColor(item.status)"
                dark
              >
                {{ item.status }}
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
      keystorePath: '',
      dialogDelete: false,
      editedIndex: -1,
      show1: false,
      agentInstances: [],
      keystoresToAdd: [],
      rules: {
        required: (value) => !!value || 'Required.',
      },
      editedItem: {
        keystorepath: '',
        password: '',
        instance: null,
        description: '',
      },
      defaultItem: {
        keystorepath: '',
        password: '',
        instances: null,
        description: '',
      },
      selected: [],
      keystores: [],
      search: '',
      headers: [
        {
          text: 'ID',
          align: 'start',
          value: 'id',
        },
        { text: 'Keystore path', value: 'location' },
        { text: 'Instance name', value: 'instanceName' },
        { text: 'Hostname', value: 'hostname' },
        { text: 'Status', value: 'status' },
        { text: 'Keystore description', value: 'description' },
      ],
    };
  },
  created() {
  },
  mounted() {
    this.getKeystoreData();
  },
  methods: {
    getKeystoreData() {
      this.$axios
        .get('http://localhost:8091/api/keystore/all-gui')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.keystores = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.agentInstances = error;
        });
    },
    addKeystore() {
      this.$axios
        .post('http://localhost:8091/api/keystore/add', this.keystoresToAdd)
        .then((response) => {
          console.log('Post response: ', response.data);
          this.getKeystoreData();
        })
        .catch((error) => {
          this.alert = true;
          this.agentInstances = error;
        });
    },
    getInstanceData() {
      this.$axios
        .get('http://localhost:8091/api/instance/all')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.agentInstances = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.agentInstances = error;
        });
    },
    getStatusColor(status) {
      if (status === 'OK') return 'green';
      if (status === 'REQUIRES ATTENTION') return 'orange';
      return 'red';
    },
    parseResponse(response) {
      const respObj = [];
      response.forEach((entry) => {
        respObj.push(entry.name);
      });
      return respObj;
    },
    editItem(item) {
      this.editedIndex = this.keystores.indexOf(item);
      this.editedItem = { ...item };
      this.dialog = true;
    },
    deleteItem(item) {
      this.editedIndex = this.keystores.indexOf(item);
      this.editedItem = { ...item };
      this.dialogDelete = true;
    },
    deleteItemConfirm() {
      this.keystores.splice(this.editedIndex, 1);
      this.closeDelete();
    },
    close() {
      this.dialog = false;
      this.$nextTick(() => {
        this.editedItem = { ...this.defaultItem };
        this.editedIndex = -1;
      });
    },
    closeDelete() {
      this.dialogDelete = false;
      this.$nextTick(() => {
        this.editedItem = { ...this.defaultItem };
        this.editedIndex = -1;
      });
    },
    save() {
      console.log(this.editedItem);
      this.keystoresToAdd = [];
      this.editedItem.instance.forEach((entry) => {
        const tempelement = {
          instanceId: entry,
          location: this.editedItem.keystorepath,
          description: this.editedItem.description,
          password: this.editedItem.password,
        };
        this.keystoresToAdd.push(tempelement);
        console.log(entry);
      });
      this.addKeystore();
      console.log(this.keystoresToAdd);
      this.close();
    },
  },
};
</script>
