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
          @click="getPayloadLocationData"
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
              Add payload location
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Add payload location</span>
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
                      v-model="newPayloadLocation.pathName"
                      label="Payload path name"
                      name="payloadPathName"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-text-field
                      v-model="newPayloadLocation.path"
                      label="Payload path"
                      name="payloadPath"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="12"
                  >
                    <v-autocomplete
                      v-model="newPayloadLocation.instanceId"
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

        <v-dialog
          v-model="uploadDialog"
          max-width="800px"
        >
          <v-card>
            <v-card-title>
              <span class="text-h5">Upload file</span>
            </v-card-title>

            <v-card-text>
              <v-container>
                <v-row>
                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-file-input
                      v-model="fileToUpload"
                      label="File input"
                    ></v-file-input>
                  </v-col>
                </v-row>
              </v-container>
            </v-card-text>

            <v-card-actions>
              <v-spacer />
              <v-btn
                color="blue darken-1"
                text
                @click="closeUpload"
              >
                Cancel
              </v-btn>
              <v-btn
                color="blue darken-1"
                text
                @click="uploadSubmit"
              >
                Upload
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

        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="showUploadDialog"
        >
          Upload to payload location
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
            :items="payloadLocations"
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
      dialog: false,
      uploadDialog: false,
      keystorePath: '',
      dialogDelete: false,
      editedIndex: -1,
      show1: false,
      agentInstances: [],
      fileToUpload:null,
      uploadToAgentData:{
        name:'',
        base64file:'',
        payloadLocationFormGUIS:[]
      },
      newPayloadLocation: {
        instanceId:'',
        pathName:'',
        path:'',
      },
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
      payloadLocations: [],
      search: '',
      headers: [
        {
          text: 'ID',
          align: 'start',
          value: 'id',
        },
        { text: 'Path name', value: 'pathName' },
        { text: 'Path', value: 'path' },
        { text: 'Instance name', value: 'instanceName' },
        { text: 'Hostname', value: 'hostname' },
      ],
    };
  },
  created() {
  },
  mounted() {
    this.getPayloadLocationData();
  },
  methods: {
    getPayloadLocationData() {
      this.$axios
        .get('http://localhost:8091/api/files/all-gui')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.payloadLocations = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.payloadLocations = error;
        });
    },
    addPayloadLocationData() {
      this.$axios
        .post('http://localhost:8091/api/files/add-location', this.newPayloadLocation)
        .then((response) => {
          console.log('Post response: ', response.data);
          this.getPayloadLocationData();
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
    closeUpload() {
      this.uploadDialog = false;
    },
    uploadSubmit(){
      this.uploadFilev2()
      this.uploadDialog=false;
    },
    showUploadDialog(){
      this.uploadDialog=true
    },
    uploadFile(){

      this.getBase64(this.fileToUpload).then(
        data => {
          this.$axios
            .post('http://localhost:8091/api/files/upload-file', data, this.selected)
            .then((response) => {
              console.log('Post response: ', response.data);
            })
            .catch((error) => {
              this.alert = true;
              console.log(error);
            });
        }
      );

      this.$axios
        .post('http://localhost:8091/api/files/upload-file', this.fileToUpload, this.selected)
        .then((response) => {
          console.log('Post response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log(error);
        });
    },
    uploadFilev2(){
      this.getBase64(this.fileToUpload).then(
        data => {
          this.uploadToAgentData.name=this.fileToUpload.name
          this.uploadToAgentData.base64file=data
          this.uploadToAgentData.payloadLocationFormGUIS=this.selected
          console.log(this.uploadToAgentData)
          this.$axios
            .post('http://localhost:8091/api/files/upload-file', this.uploadToAgentData)
            .then((response) => {
              console.log('Post response: ', response.data);
            })
            .catch((error) => {
              this.alert = true;
              console.log(error);
            });
        }
      );
    },
    getBase64(file){
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
          let encoded = reader.result.toString().replace(/^data:(.*,)?/, '');
          if ((encoded.length % 4) > 0) {
            encoded += '='.repeat(4 - (encoded.length % 4));
          }
          resolve(encoded);
        };
        reader.onerror = error => reject(error);
      });
    },
    save() {
      this.newPayloadLocation.instanceId=this.newPayloadLocation.instanceId[0]
      console.log(this.newPayloadLocation)
      this.addPayloadLocationData();
      this.newPayloadLocation.instanceId=null
      this.close();
    },
  },
};
</script>
