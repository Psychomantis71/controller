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
          @click="postData"
        >
          Force refresh
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="postData"
        >
          Add
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="postData"
        >
          Replace
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="postData"
        >
          Renew
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="postData"
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
            :items="instances"
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
            <template v-slot:expanded-item="{ headers, item }">
              <td :colspan="headers.length">
                More info about {{ item.name }}
                <br>
                Valid from: 20.12.2020
                <br>
                Valid to: 20.12.2020
                <br>
                Subject: CN=ASDF, OU=ASDF, O=ASDF, L=Zagreb, C=HR
                <br>
                Issuer: CN=ASDF, OU=ASDF, O=ASDF, L=Zagreb, C=HR
                <br>
                Serial: 45-23-78-BB-12-4A-EE-12-44-00-00
                <br>
                Thumbprint: SD678SMOL098H5K34F1ASD
              </td>
            </template>
          </v-data-table>
        </v-card>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="getSecuredUserInformation"
        >
          Call secured user service
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="getSecuredAdminInformation"
        >
          Call secured admin service
        </v-btn>
        <v-btn
          dark
          color="teal lighten-1"
          class="ma-2"
          @click="postData"
        >
          Make secured POST request
        </v-btn>
      </v-flex>

      <v-flex
        xs8
        offset-xs2
        class="text-left"
        mt-5
      >
        <h2>Request URL: {{ responseObj.url }}</h2>
        <h2>Request method: {{ responseObj.method }}</h2>
        <h2>Status code: {{ responseObj.statusCode }}</h2>
        <h2>Response: {{ responseObj.msg }}</h2>
        <h2>X-XSRF-TOKEN: {{ responseObj.xsrfToken }}</h2>
      </v-flex>
    </v-layout>
  </v-container>
</template>

<script>
export default {
  data() {
    return {
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
        { text: 'Certificate alias', value: 'name' },
        { text: 'Keystore path', value: 'keystorepath' },
        { text: 'Instance name', value: 'instancename' },
        { text: 'Hostname', value: 'hostname' },
        { text: 'Managed', value: 'managed' },
        { text: 'Status', value: 'status' },
        { text: '', value: 'data-table-expand' },
      ],
      instances: [
        {
          id: '0',
          name: 'cert_alias_1',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exwasdmgr1',
          hostname: 'exwasdmgr1.lan',
          managed: 'YES',
          status: 'VALID',
        },
        {
          id: '1',
          name: 'cert_alias_2',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exwas1',
          hostname: 'exwas1.lan',
          managed: 'NO',
          status: 'VALID',
        },
        {
          id: '2',
          name: 'cert_alias_3',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exwas2',
          hostname: 'exwas2.lan',
          managed: 'YES',
          status: 'EXPIRING SOON',
        },
        {
          id: '3',
          name: 'cert_alias_4',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exweb1',
          hostname: 'exweb1.lan',
          managed: 'NO',
          status: 'VALID',
        },
        {
          id: '4',
          name: 'cert_alias_5',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exweb2',
          hostname: 'exweb2.lan',
          managed: 'NO',
          status: 'VALID',
        },
        {
          id: '5',
          name: 'cert_alias_6',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exwebsvc1',
          hostname: 'exwebsvc1.lan',
          managed: 'NO',
          status: 'VALID',
        },
        {
          id: '6',
          name: 'cert_alias_7',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exwebsvc1',
          hostname: 'exwebsvc2.lan',
          managed: 'NO',
          status: 'VALID',
        },
        {
          id: '7',
          name: 'cert_alias_8',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exldap1',
          hostname: 'exldap1.lan',
          managed: 'NO',
          status: 'VALID',
        },
        {
          id: '8',
          name: 'cert_alias_9',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'exldap2',
          hostname: 'exldap2.lan',
          managed: 'YES',
          status: 'VALID',
        },
        {
          id: '9',
          name: 'cert_alias_10',
          keystorepath: '/usr/bin/java/lib/security/cacerts',
          instancename: 'kurac',
          hostname: 'kurac.lan',
          managed: 'NO',
          status: 'EXPIRED',
        },
      ],
    };
  },
  created() {
  },
  methods: {
    getSecuredUserInformation() {
      this.responseObj = {};
      this.$axios
        .get('http://localhost:8091/secured/welcome')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.responseObj = this.parseResponse(response);
        })
        .catch((error) => {
          this.alert = true;
          this.responseObj = this.parseResponse(error);
        });
    },
    getSecuredAdminInformation() {
      this.responseObj = {};
      this.$axios
        .get('http://localhost:8091/onlyforadmin/welcome')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.responseObj = this.parseResponse(response);
        })
        .catch((error) => {
          this.alert = true;
          this.responseObj = this.parseResponse(error);
        });
    },
    postData() {
      this.responseObj = {};
      this.$axios
        .post('http://localhost:8091/secured/postdata')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.responseObj = this.parseResponse(response);
        })
        .catch((error) => {
          this.alert = true;
          this.responseObj = this.parseResponse(error);
        });
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
    getStatusColor(status) {
      if (status === 'VALID') return 'green';
      if (status === 'EXPIRING SOON') return 'orange';
      return 'red';
    },
    getManagedColor(status) {
      if (status === 'YES') return 'green';
      return 'red';
    },
  },
};
</script>
