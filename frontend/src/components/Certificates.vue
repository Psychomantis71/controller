<template>
  <v-container>
    <v-layout row>
      <v-flex
        xs12
        class="text-center"
        mt-5
      >
        <h1>Certificates</h1>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="getCertificateData"
        >
          Force refresh
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="exportCertificatePEM"
        >
          Export
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
        >
          Renew
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="removeCertificates"
        >
          Delete
        </v-btn>
        <v-dialog
          v-model="dialogImportCert"
          max-width="1000px"
        >
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              dark
              color="teal lighten-1"
              class="ma-2"
              v-bind="attrs"
              v-on="on"
              @click="getKeystoreData"
            >
              Import certificates
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Import certificate</span>
            </v-card-title>

            <v-card-text>
              <v-container>
                <v-row>
                  <v-col
                    cols="12"
                    sm="6"
                    md="6"
                  >
                  <v-select
                    v-model="importItem.importFormat"
                    :items="importOptions"
                    label="Select import format"
                    item-value="choiceValue"
                    item-text="choiceName"
                  ></v-select>
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="6"
                  >
                  <v-text-field
                    v-model="importItem.password"
                    v-if="importItem.importFormat === 'PKCS12'"
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
                </v-row>
                <v-row>
                  <v-file-input
                    v-model="fileToUpload"
                    label="File input"
                  ></v-file-input>


                  <v-card>
                  <v-card-title>
                  <v-text-field
                    v-model="searchKeystore"
                    append-icon="mdi-magnify"
                    label="Search"
                    single-line
                    hide-details
                  />
                  </v-card-title>
                  <v-data-table
                    v-model="importItem.selectedKeystores"
                    :headers="headersKeystore"
                    :items="keystores"
                    :search="searchKeystore"
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
                </v-row>
              </v-container>
            </v-card-text>

            <v-card-actions>
              <v-spacer />
              <v-btn
                color="blue darken-1"
                text
                @click="closeImportCert"
              >
                Cancel
              </v-btn>
              <v-btn
                color="blue darken-1"
                text
                @click="saveImportCert"
              >
                Save
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
        <v-dialog
          v-model="dialogRetrieveFromPort"
          max-width="1000px"
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
              Retrieve from port
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Retrieve from port</span>
            </v-card-title>

            <v-card-text>
              <v-container>
                <v-row>
                  <v-col
                    cols="12"
                    sm="6"
                    md="6"
                  >
                    <v-select
                      v-model="retrieveItem.fromController"
                      :items="retrievalFromOptions"
                      label="Try retrival from:"
                      item-value="choiceValue"
                      item-text="choiceName"
                    ></v-select>
                    <v-autocomplete
                      v-model="retrieveItem.instanceId"
                      :items="instances"
                      outlined
                      dense
                      chips
                      small-chips
                      label="Select instance"
                      item-value="id"
                      item-text="hostname"
                      v-if="!retrieveItem.fromController"
                    />
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="6"
                  >
                    <v-text-field
                      v-model="retrieveItem.hostname"
                      label="Hostname or IP"
                      name="hostnameField"
                    />
                    <v-text-field
                      v-model="retrieveItem.port"
                      label="Port"
                      name="portField"
                    />
                  </v-col>
                </v-row>
                <v-row
                >
                  <v-data-table
                    v-model="retrieveFromSelected"
                    :headers="retrieveFromHeaders"
                    :items="retrieveFromItems"
                    :search="search"
                    :expanded.sync="expanded"
                    show-select
                    show-expand
                    class="elevation-1"
                    item-key="id"
                    v-if="retrieveFromItems!==null"
                  >
                    <template v-slot:item.status="{ item }">
                      <v-chip
                        :color="getStatusColor(item.status)"
                        dark
                      >
                        {{ item.status }}
                      </v-chip>
                    </template>
                    <template v-slot:expanded-item="{ item }">
                      <td :colspan="headers.length">
                        Subject: {{ item.subject }}
                        <br>
                        Issuer: {{ item.issuer }}
                        <br>
                        Valid from: {{ item.validFrom }}
                        <br>
                        Valid to: {{ item.validTo }}
                        <br>
                        Serial: {{ item.serial }}
                        <br>
                        <div v-if="item.alternativeNameDNS !== null && item.alternativeNameDNS !=='' ">
                          <v-chip>Alternative DNS: {{ item.alternativeNameDNS }}</v-chip>
                          <br>
                        </div>
                        <div v-if="item.alternativeNameIP !== null && item.alternativeNameIP !=='' ">
                          <v-chip>Alternative IP: {{ item.alternativeNameIP }}</v-chip>
                          <br>
                        </div>
                        Keysize: {{ item.keysize }}
                        <br>
                        Signature: {{ item.signature }}
                        <br>
                        Signature Hash Algorithm: {{ item.signatureHashAlgorithm }}
                      </td>
                    </template>
                  </v-data-table>
                </v-row>
              </v-container>
            </v-card-text>

            <v-card-actions>
              <v-spacer />
              <v-btn
                color="blue darken-1"
                text
                @click="closeRetrieveFrom"
              >
                Cancel
              </v-btn>
              <v-btn
                color="blue darken-1"
                text
                @click="exportCertificateZip"
              >
                Download
              </v-btn>
              <v-btn
                color="blue darken-1"
                text
                @click="tryRetrieveFrom"
              >
                Retrieve
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
            />
          </v-card-title>
          <v-data-table
            v-model="selected"
            :headers="headers"
            :items="certificatelist"
            :search="search"
            :expanded.sync="expanded"
            item-key="id"
            show-select
            show-expand
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
            <template v-slot:item.managed="{ item }">
              <v-chip
                :color="getManagedColor(item.managed)"
                dark
              >
                {{ item.managed }}
              </v-chip>
            </template>
            <template v-slot:expanded-item="{ item }">
              <td :colspan="headers.length">
                Subject: {{ item.subject }}
                <br>
                Issuer: {{ item.issuer }}
                <br>
                Valid from: {{ item.validFrom }}
                <br>
                Valid to: {{ item.validTo }}
                <br>
                Serial: {{ item.serial }}
                <br>
                <div v-if="item.alternativeNameDNS !== null && item.alternativeNameDNS !=='' ">
                  <v-chip>Alternative DNS: {{ item.alternativeNameDNS }}</v-chip>
                  <br>
                </div>
                <div v-if="item.alternativeNameIP !== null && item.alternativeNameIP !=='' ">
                  <v-chip>Alternative IP: {{ item.alternativeNameIP }}</v-chip>
                  <br>
                </div>
                Keysize: {{ item.keysize }}
                <br>



                Key usage: {{ item.keyUsage }}
                <br>
                Signature: {{ item.signature }}
                <br>
                Signature Hash Algorithm: {{ item.signatureHashAlgorithm }}
                <br>
                Private key present in database: {{ item.privateKey }}
              </td>
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
      instances:[],
      certificatelist: [],
      fileToUpload:null,
      dialogImportCert:false,
      keystores: [],
      show1: false,
      selected: [],
      retrieveFromSelected:[],
      retrieveFromItems:null,
      rules: {
        required: (value) => !!value || 'Required.',
      },
      search: '',
      headers: [
        {
          text: 'ID',
          align: 'start',
          value: 'id',
        },
        { text: 'Subject', value: 'subject' },
        { text: 'Managed', value: 'managed' },
        { text: 'Status', value: 'status' },
        { text: '', value: 'data-table-expand' },
      ],
      retrieveFromHeaders: [
        {
          text: 'ID',
          align: 'start',
          value: 'id',
        },
        { text: 'Subject', value: 'subject' },
        { text: 'Status', value: 'status' },
        {text:'Valid to', value: 'validTo'},
        { text: '', value: 'data-table-expand' },
      ],
      importOptions:[ { choiceName: 'PEM certificate', choiceValue: 'PEM' },
        { choiceName: 'PKCS12', choiceValue: 'PKCS12' },
      ],
      retrievalFromOptions:[ { choiceName: 'Controller', choiceValue: true },
        { choiceName: 'Instance', choiceValue: false },
      ],
      dialogRetrieveFromPort:false,
      retrieveItem:{
        fromController: true,
        save:false,
        download: false,
        port:443,
        hostname:"localhost",
        instanceId:null,
        keystoreFormGUIS:[]
      },
      importFormat:[],
      searchKeystore: '',
      importItem: {
        selectedKeystores: null,
        password: '',
        importFormat: '',
        filename:'',
        base64File:'',
      },
      headersKeystore: [
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
    this.getCertificateData();
  },
  methods: {
    getCertificateData() {
      this.$axios
        .get('http://localhost:8091/api/certificate/all-gui')
        .then((response) => {
          console.log(response.data)
          this.certificatelist = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.certificatelist = error;
        });
    },
    removeCertificates() {
      this.$axios
        .post('http://localhost:8091/api/certificate/remove',this.selected)
        .then((response) => {
          console.log(response)
        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
      this.getCertificateData();
    },
    exportCertificatePEM() {
      this.$axios
        .get(`http://localhost:8091/api/certificate/${this.selected[0].id}/export-pem`)
        .then((response) => {
          let filetodownload = response.headers['content-disposition'].split('filename=')[1].split(';')[0];
          filetodownload = filetodownload.substring(1, filetodownload.length-1)
          console.log(filetodownload);
          var fileURL = window.URL.createObjectURL(new Blob([response.data]));
          var fileLink = document.createElement('a');

          fileLink.href = fileURL;
          fileLink.setAttribute('download', filetodownload);
          document.body.appendChild(fileLink);

          fileLink.click();

        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
    },
    exportCertificateZip() {
      this.$axios
        .post('http://localhost:8091/api/certificate/retrieve-and-download', this.retrieveItem, {
          responseType: 'blob'
        })
        .then((response) => {
          let filetodownload = response.headers['content-disposition'].split('filename=')[1].split(';')[0];
          filetodownload = filetodownload.substring(1, filetodownload.length-1)
          console.log(filetodownload);
          var fileURL = window.URL.createObjectURL(new Blob([response.data], {type: 'application/zip'}));
          var fileLink = document.createElement('a');

          fileLink.href = fileURL;
          fileLink.setAttribute('download', filetodownload);
          document.body.appendChild(fileLink);

          fileLink.click();

        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
    },
    getStatusColor(status) {
      if (status === 'VALID') return 'green';
      if (status === 'EXPIRING SOON') return 'orange';
      if (status === 'NOT YET VALID') return 'orange';
      return 'red';
    },
    getManagedColor(status) {
      if (status === true) return 'green';
      return 'red';
    },
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
    uploadFile(){
      this.getBase64(this.fileToUpload).then(
        data => {
          this.importItem.base64File=data
          this.importItem.filename=this.fileToUpload.name
          console.log(this.importItem)
          this.$axios
            .post('http://localhost:8091/api/keystore-certificate/import', this.importItem)
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
    getInstanceData() {
      this.$axios
        .get('http://localhost:8091/api/instance/all')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.instances = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.instances = error;
        });
    },
    retrieveFromPortData() {
      this.$axios
        .post('http://localhost:8091/api/certificate/retrieve-from-port',this.retrieveItem)
        .then((response) => {
          this.retrieveFromItems = response.data;
          for (let index = 0; index < this.retrieveFromItems.length; ++index) {
            this.retrieveFromItems[index].validTo = this.retrieveFromItems[index].validTo.substring(0,10)
          }


          console.log(response)
        })
        .catch((error) => {
          this.alert = true;
          console.log(error)
        });
    },
    saveImportCert() {
      this.uploadFile();
      this.closeImportCert();
    },
    closeImportCert() {
      this.dialogImportCert = false;
    },
    closeRetrieveFrom() {
      this.dialogRetrieveFromPort = false;
    },
    tryRetrieveFrom() {
      this.retrieveFromPortData();
      console.log(this.retrieveItem)
    },
  },
};
</script>
