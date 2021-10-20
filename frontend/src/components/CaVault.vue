<template>
  <v-container>
    <v-layout row>
      <v-flex
        xs12
        class="text-center"
        mt-5
      >
        <h1>CA vault</h1>
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
        >
          Add
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
        >
          Replace
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
      >
        Delete
      </v-btn>
        <v-dialog
          v-model="dialog"
          max-width="1000px"
        >
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              dark
              color="teal lighten-1"
              class="ma-2"
              v-bind="attrs"
              v-on="on"
            >
              Create root certificate
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Create root certificate</span>
            </v-card-title>

            <v-card-text>
              <v-container>

                <v-stepper v-model="e1">
                  <v-stepper-header>
                    <v-stepper-step
                      :complete="e1 > 1"
                      step="1"
                    >
                      Select key and signature algorithm
                    </v-stepper-step>

                    <v-divider></v-divider>

                    <v-stepper-step
                      :complete="e1 > 2"
                      step="2"
                    >
                      Select keysize and validity period
                    </v-stepper-step>

                    <v-divider></v-divider>

                    <v-stepper-step step="3">
                      Common name and extension
                    </v-stepper-step>
                  </v-stepper-header>

                  <v-stepper-items>
                    <v-stepper-content step="1">
                      <v-col
                        class="d-flex"
                        cols="12"
                        sm="6"
                      >
                        <v-select
                          v-model="newCa.keyAlgorithm"
                          :items="keyAlgorithmItems"
                          label="Key algorithm"
                        ></v-select>
                      </v-col>
                      <v-col
                        class="d-flex"
                        cols="12"
                        sm="6"
                      >
                        <v-select
                          v-model="newCa.signatureAlgorithm"
                          :items="signatureAlgorithmItems"
                          label="Signature algorithm"
                        ></v-select>
                      </v-col>


                      <v-btn
                        color="primary"
                        @click="e1 = 2"
                      >
                        Continue
                      </v-btn>

                      <v-btn
                        text
                        @click="close"
                      >
                        Cancel
                      </v-btn>
                    </v-stepper-content>

                    <v-stepper-content step="2">
                      <v-col
                        cols="12"
                        sm="6"
                        md="4"
                      >
                        <v-select
                          v-model="newCa.keySize"
                          :items="keySizeItems"
                          label="Key size"
                        ></v-select>
                      </v-col>
                      <v-col
                        cols="12"
                        sm="6"
                        md="4"
                      >
                        <v-menu
                          v-model="fromMenu"
                          :close-on-content-click="false"
                          :nudge-right="40"
                          transition="scale-transition"
                          offset-y
                          min-width="auto"
                        >
                          <template v-slot:activator="{ on, attrs }">
                            <v-text-field
                              v-model="newCa.dateFrom"
                              label="Valid from:"
                              prepend-icon="mdi-calendar"
                              readonly
                              v-bind="attrs"
                              v-on="on"
                            ></v-text-field>
                          </template>
                          <v-date-picker
                            v-model="newCa.dateFrom"
                            @input="fromMenu = false"
                          ></v-date-picker>
                        </v-menu>
                      </v-col>
                      <v-col
                        cols="12"
                        sm="6"
                        md="4"
                      >
                        <v-menu
                          v-model="toMenu"
                          :close-on-content-click="false"
                          :nudge-right="40"
                          transition="scale-transition"
                          offset-y
                          min-width="auto"
                        >
                          <template v-slot:activator="{ on, attrs }">
                            <v-text-field
                              v-model="newCa.dateTo"
                              label="Valid from:"
                              prepend-icon="mdi-calendar"
                              readonly
                              v-bind="attrs"
                              v-on="on"
                            ></v-text-field>
                          </template>
                          <v-date-picker
                            v-model="newCa.dateTo"
                            @input="toMenu = false"
                          ></v-date-picker>
                        </v-menu>
                      </v-col>
                      <v-btn
                        color="primary"
                        @click="e1 = 3"
                      >
                        Continue
                      </v-btn>

                      <v-btn
                        text
                        @click="close"
                      >
                        Cancel
                      </v-btn>
                    </v-stepper-content>

                    <v-stepper-content step="3">
                      <v-col
                        cols="12"
                        sm="6"
                        md="3"
                      >
                        <v-text-field
                          v-model="newCa.commonName"
                          label="Common name"
                        ></v-text-field>

                      </v-col>
                      <v-col
                        cols="12"
                        sm="6"
                        md="3"
                      >
                        <v-text-field
                          v-model="newCa.certAlias"
                          label="Alias"
                        ></v-text-field>

                      </v-col>

                      <v-btn
                        color="primary"
                        @click="postNewCa"
                      >
                        Finish
                      </v-btn>

                      <v-btn
                        text
                        @click="close"
                      >
                        Cancel
                      </v-btn>
                    </v-stepper-content>
                  </v-stepper-items>
                </v-stepper>
              </v-container>
            </v-card-text>
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
                Details about {{ item.alias }}
                <br>
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
      e1: 1,
      dialog: false,
      newCa:{
        keyAlgorithm: '',
        signatureAlgorithm: '',
        keySize: '',
        dateFrom: (new Date(Date.now() - (new Date()).getTimezoneOffset() * 60000)).toISOString().substr(0, 10),
        dateTo: (new Date(Date.now() - (new Date()).getTimezoneOffset() * 60000)).toISOString().substr(0, 10),
        commonName: '',
        certAlias: '',
      },
      keyAlgorithmItems: ['RSA'],
      signatureAlgorithmItems: ['SHA256withRSA'],
      keySizeItems: ['2048','3072','4096'],
      fromMenu: false,
      toMenu:false,
      certificatelist: [],
      selected: [],
      search: '',
      headers: [
        {
          text: 'ID',
          align: 'start',
          value: 'id',
        },
        { text: 'Certificate alias', value: 'alias' },
        { text: 'Managed', value: 'managed' },
        { text: 'Status', value: 'status' },
        { text: '', value: 'data-table-expand' },
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
        .get('http://localhost:8091/api/cavault/all-gui')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.certificatelist = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.certificatelist = error;
        });
    },
    postNewCa() {
      this.$axios
        .post('http://localhost:8091/api/cavault/add-root', this.newCa)
        .then((response) => {
          console.log('Post response: ', response.data);
          this.getCertificateData();
        })
        .catch((error) => {
          this.alert = true;
          console.log('Error while trying to create new root cert:',error);
        });

      console.log('CA data:', this.newCa)
      this.close()
    },
    getStatusColor(status) {
      if (status === 'VALID') return 'green';
      if (status === 'EXPIRING SOON') return 'orange';
      if (status === 'NOT YET VALID') return 'blue';
      return 'red';
    },
    getManagedColor(status) {
      if (status === 'YES') return 'green';
      return 'red';
    },
    close() {
      this.dialog = false;
    },
  },
};
</script>
