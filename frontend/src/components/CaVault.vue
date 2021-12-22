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

        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="sizeOfSelectedReplace"
        >
          Replace
        </v-btn>


        <v-dialog
          v-model="dialogReplace"
          max-width="1000px"
        >
          <v-card>
            <v-card-title>
              <span class="text-h5">Replace certificate</span>
            </v-card-title>

            <v-card-text>
              <v-container>
                <v-row>
                  <p> By replacing a certificate you are keeping the dependency chain/any managed certificates will still renew from this certificate </p>
                </v-row>
                <v-row>
                  <v-file-input
                    v-model="fileToUpload"
                    label="PEM input file"
                  ></v-file-input>
                </v-row>
              </v-container>
            </v-card-text>

            <v-card-actions>
              <v-spacer />
              <v-btn
                color="blue darken-1"
                text
                @click="closeReplaceCert"
              >
                Cancel
              </v-btn>
              <v-btn
                color="blue darken-1"
                text
                @click="saveReplaceCert"
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
          @click="sizeOfSelectedExport"
        >
          Export
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="sizeOfSelectedSigned"
        >
          Create signed certificate
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



        <v-dialog
          v-model="assignSignerDialog"
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
              Assign signer
            </v-btn>
          </template>
          <v-card>
            <v-card-title>
              <span class="text-h5">Select a signer</span>
            </v-card-title>

            <v-card-text>
              <v-container>

                <v-card>
                  <v-card-title>
                    <v-text-field
                      v-model="searchSigner"
                      append-icon="mdi-magnify"
                      label="Search"
                      single-line
                      hide-details
                    />
                  </v-card-title>
                  <v-data-table
                    v-model="selectedSigners"
                    :headers="headers"
                    :items="certificatelist"
                    :search="searchSigner"
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
                    <template v-slot:item.level="{ item }">
                      <v-chip
                        :color="getLevelColour(item.level)"
                        dark
                      >
                        {{ item.level }}
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

              </v-container>
            </v-card-text>

            <v-card-actions>
              <v-spacer />
              <v-btn
                color="blue darken-1"
                text
                @click="closeAssignedSigner"
              >
                Cancel
              </v-btn>
              <v-btn
                color="blue darken-1"
                text
                @click="submitAssignedSigner"
              >
                Assign
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="removeCert"
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
            <template v-slot:item.level="{ item }">
              <v-chip
                :color="getLevelColour(item.level)"
                dark
              >
                {{ item.level }}
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



        <v-dialog
          v-model="dialogSigned"
          max-width="1000px"
        >
          <v-card>
            <v-card-title>
              <span class="text-h5">Create signed certificate</span>
            </v-card-title>


            <v-stepper v-model="e1">
              <v-stepper-header>
                <template v-for="n in steps">
                  <v-stepper-step
                    :key="`${n}-step`"
                    :complete="e1 > n"
                    :step="n"
                    editable

                  >
                    Random shit
                  </v-stepper-step>

                  <v-divider
                    v-if="n < steps.length"
                    :key="n"
                  ></v-divider>
                </template>
              </v-stepper-header>

              <v-stepper-items>
                <v-stepper-content
                  step="1"
                  :key="`1-content`"
                >
                  <v-card-text>
                    <v-select
                      v-model="steps"
                      :items="testItem"
                      label="Select certificate destination"
                      item-value="choiceValue"
                      item-text="choiceName"
                    ></v-select>
                  </v-card-text>
                  <v-btn
                    color="primary"
                    @click="e1 = 2"
                  >
                    Continue
                  </v-btn>

                  <v-btn
                    text
                    @click="closeSigned"
                  >
                    Cancel
                  </v-btn>
                </v-stepper-content>

                <v-stepper-content
                  step="2"
                  :key="`2-content`"
                >

                  <v-col
                    class="d-flex"
                    cols="12"
                    sm="6"
                  >
                    <v-select
                      v-model="newSignedCert.keyAlgorithm"
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
                      v-model="newSignedCert.signatureAlgorithm"
                      :items="signatureAlgorithmItems"
                      label="Signature algorithm"
                    ></v-select>
                  </v-col>


                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-select
                      v-model="newSignedCert.keySize"
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
                      v-model="fromMenuSigned"
                      :close-on-content-click="false"
                      :nudge-right="40"
                      transition="scale-transition"
                      offset-y
                      min-width="auto"
                    >
                      <template v-slot:activator="{ on, attrs }">
                        <v-text-field
                          v-model="newSignedCert.dateFrom"
                          label="Valid from:"
                          prepend-icon="mdi-calendar"
                          readonly
                          v-bind="attrs"
                          v-on="on"
                        ></v-text-field>
                      </template>
                      <v-date-picker
                        v-model="newSignedCert.dateFrom"
                        @input="fromMenuSigned = false"
                      ></v-date-picker>
                    </v-menu>
                  </v-col>
                  <v-col
                    cols="12"
                    sm="6"
                    md="4"
                  >
                    <v-menu
                      v-model="toMenuSigned"
                      :close-on-content-click="false"
                      :nudge-right="40"
                      transition="scale-transition"
                      offset-y
                      min-width="auto"
                    >
                      <template v-slot:activator="{ on, attrs }">
                        <v-text-field
                          v-model="newSignedCert.dateTo"
                          label="Valid from:"
                          prepend-icon="mdi-calendar"
                          readonly
                          v-bind="attrs"
                          v-on="on"
                        ></v-text-field>
                      </template>
                      <v-date-picker
                        v-model="newSignedCert.dateTo"
                        @input="toMenuSigned = false"
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
                    @click="closeSigned"
                  >
                    Cancel
                  </v-btn>
                </v-stepper-content>

                <v-stepper-content
                  step="3"
                  :key="`3-content`"
                >

                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.certAlias"
                      label="Alias"
                    ></v-text-field>
                  </v-col>

                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.commonName"
                      label="Common name"
                    ></v-text-field>
                  </v-col>

                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.organization"
                      label="Organization"
                    ></v-text-field>
                  </v-col>

                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.organizationalUnit"
                      label="Organizational Unit"
                    ></v-text-field>
                  </v-col>

                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.locality"
                      label="Locality"
                    ></v-text-field>
                  </v-col>

                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.countryName"
                      label="Country name"
                    ></v-text-field>
                  </v-col>

                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.stateOrProvinceName"
                      label="State or Province"
                    ></v-text-field>
                  </v-col>


                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.dnsname"
                      label="DNS name"
                    ></v-text-field>
                  </v-col>


                  <v-col
                    cols="12"
                    sm="6"
                    md="3"
                  >
                    <v-text-field
                      v-model="newSignedCert.ipaddres"
                      label="IP address"
                    ></v-text-field>
                  </v-col>

                  <v-btn
                    color="primary"
                    @click="postNewSigned"
                  >
                    Finish
                  </v-btn>

                  <v-btn
                    text
                    @click="closeSigned"
                  >
                    Cancel
                  </v-btn>
                </v-stepper-content>

                <v-stepper-content
                  step="4"
                  :key="`4-content`"
                >
                  <v-card
                    class="mb-12"
                    color="grey lighten-1"
                    height="200px"
                  ></v-card>

                  <v-btn
                    color="primary"
                    @click="e1 = 5"
                  >
                    Continue
                  </v-btn>

                  <v-btn text>
                    Cancel
                  </v-btn>
                </v-stepper-content>

                <v-stepper-content
                  step="5"
                  :key="`5-content`"
                >
                  <v-card
                    class="mb-12"
                    color="grey lighten-1"
                    height="200px"
                  ></v-card>

                  <v-btn
                    color="primary"
                    @click="e1 = 1"
                  >
                    Continue
                  </v-btn>

                  <v-btn text>
                    Cancel
                  </v-btn>
                </v-stepper-content>
              </v-stepper-items>
            </v-stepper>


          </v-card>
        </v-dialog>



        <v-snackbar
          v-model="snackbar"
          :timeout="timeout"
        >
          You need to select one certificate
          <template v-slot:action="{ attrs }">
            <v-btn
              color="red"
              text
              v-bind="attrs"
              @click="snackbar = false"
            >
              Close
            </v-btn>
          </template>
        </v-snackbar>


      </v-flex>
    </v-layout>
  </v-container>
</template>

<style>
.v-stepper__step:not(.v-stepper__step--active):not(.v-stepper__step--complete):not(.v-stepper__step--error) .v-stepper__step__step {
  color: transparent;
}
.v-stepper__step--active:not(.v-stepper__step--complete) .v-stepper__step__step {
  color: transparent;
}
</style>

<script>
export default {
  data() {
    return {
      fileToUpload:null,
      dialogImportCert:false,
      assignSignerDialog:false,
      show1: false,
      importItem: {
        password: '',
        importFormat: '',
        filename:'',
        base64File:'',
      },
      importOptions:[ { choiceName: 'PEM certificate', choiceValue: 'PEM' },
        { choiceName: 'PKCS12', choiceValue: 'PKCS12' },
      ],
      rules: {
        required: (value) => !!value || 'Required.',
      },
      dialogReplace:false,
      e1: 1,
      snackbar: false,
      timeout: 4000,
      dialog: false,
      dialogSigned: false,
      steps: [1,2,3],
      testItem:[ { choiceName: 'Create and deploy directly to keystore(s)', choiceValue: [1, 2, 4] },
        { choiceName: 'Create a intermediate certificate', choiceValue: [1, 2, 3] },
        { choiceName: 'Create and export directly', choiceValue: [1, 2, 3, 4, 5] },
      ],
      newCa:{
        keyAlgorithm: '',
        signatureAlgorithm: '',
        keySize: '',
        dateFrom: (new Date(Date.now() - (new Date()).getTimezoneOffset() * 60000)).toISOString().substr(0, 10),
        dateTo: (new Date(Date.now() - (new Date()).getTimezoneOffset() * 60000)).toISOString().substr(0, 10),
        commonName: '',
        certAlias: '',
      },
      newSignedCert:{
        keyAlgorithm: '',
        signatureAlgorithm: '',
        keySize: '',
        dateFrom: (new Date(Date.now() - (new Date()).getTimezoneOffset() * 60000)).toISOString().substr(0, 10),
        dateTo: (new Date(Date.now() - (new Date()).getTimezoneOffset() * 60000)).toISOString().substr(0, 10),
        commonName: '',
        organizationalUnit: '',
        organization: '',
        locality: '',
        stateOrProvinceName: '',
        countryName: '',
        dnsname: '',
        ipaddres: '',
        certAlias: '',
        intermediate:'',
        signingCertId:'',
      },
      keyAlgorithmItems: ['RSA'],
      signatureAlgorithmItems: ['SHA256withRSA'],
      keySizeItems: ['2048','3072','4096'],
      fromMenu: false,
      toMenu:false,
      fromMenuSigned: false,
      toMenuSigned:false,
      certificatelist: [],
      selected: [],
      selectedSigners: [],
      search: '',
      searchSigner:'',
      headers: [
        {
          text: 'ID',
          align: 'start',
          value: 'id',
        },
        { text: 'Certificate alias', value: 'alias' },
        { text: 'Managed', value: 'managed' },
        { text: 'Status', value: 'status' },
        { text: 'Type', value: 'level' },
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
    //This is horrible i know, will fix it when i figure out how to do it properly
    sizeOfSelectedSigned(){
      if( this.selected.length !== 1 ){
        this.snackbar = true
      }else{
        this.dialogSigned=true
      }
    },
    sizeOfSelectedExport(){
      if( this.selected.length !== 1 ){
        this.snackbar = true
      }else{
        this.exportCertificatePEM()
      }
    },
    sizeOfSelectedReplace(){
      if( this.selected.length !== 1 ){
        this.snackbar = true
      }else{
        this.dialogReplace = true;
      }
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
    postAssignerSigner() {
      this.$axios
        .post(`http://localhost:8091/api/cavault/${this.selectedSigners[0].id}/cacert-assign-signer`, this.selected)
        .then((response) => {
          console.log('Post response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log('Error while trying to create new root cert:',error);
        });

      console.log('CA data:', this.newCa)
      this.close()
    },
    postNewSigned() {
      this.newSignedCert.intermediate = true
      this.newSignedCert.signingCertId = this.selected[0].id
      this.e1 = 1
      this.$axios
        .post('http://localhost:8091/api/cavault/add-signed', this.newSignedCert)
        .then((response) => {
          console.log('Post response: ', response.data);
          this.getCertificateData();
        })
        .catch((error) => {
          this.alert = true;
          console.log('Error while trying to create new signed cert:', error);
        });

      console.log('New signed cert data:', this.newSignedCert)
      this.close()
    },
    exportCertificatePEM() {
      this.$axios
        .get(`http://localhost:8091/api/cavault/${this.selected[0].id}/export-pem`)
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
    getStatusColor(status) {
      if (status === 'VALID') return 'green';
      if (status === 'EXPIRING SOON') return 'orange';
      if (status === 'NOT YET VALID') return 'blue';
      return 'red';
    },
    getManagedColor(status) {
      if (status === true) return 'green';
      return 'red';
    },
    getLevelColour(level) {
      if (level==='ROOT') return 'purple';
      return 'pink';
    },
    close() {
      this.dialog = false;
      this.e1=1
    },
    closeSigned() {
      this.dialogSigned = false;
      this.e1=1
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
            .post('http://localhost:8091/api/cavault/import', this.importItem)
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
    uploadFileReplace(){
      this.getBase64(this.fileToUpload).then(
        data => {
          this.importItem.base64File=data
          this.importItem.filename=this.fileToUpload.name
          console.log(this.importItem)
          this.$axios
            .post(`http://localhost:8091/api/cavault/${this.selected[0].id}/replace`, this.importItem)
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
    removeCert() {
      this.$axios
        .post('http://localhost:8091/api/cavault/remove', this.selected)
        .then((response) => {
          console.log('Post response: ', response.data);
        })
        .catch((error) => {
          this.alert = true;
          console.log('Error deleting certificates: ',error);
        });
    },
    saveImportCert() {
      this.uploadFile();
      this.closeImportCert();
    },
    closeImportCert() {
      this.dialogImportCert = false;
    },
    saveReplaceCert() {
      this.uploadFileReplace();
      this.closeReplaceCert();
    },
    closeReplaceCert() {
      this.dialogReplace = false;
    },
    submitAssignedSigner() {
      this.postAssignerSigner();
      this.closeAssignedSigner();
    },
    closeAssignedSigner() {
      this.assignSignerDialog = false;
    },
  },
};
</script>
