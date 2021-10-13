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
        { text: 'Keystore path', value: 'keystorePath' },
        { text: 'Instance name', value: 'instanceName' },
        { text: 'Hostname', value: 'hostname' },
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
        .get('http://localhost:8091/api/certificate/all-gui')
        .then((response) => {
          console.log('Get response: ', response.data);
          this.certificatelist = response.data;
        })
        .catch((error) => {
          this.alert = true;
          this.certificatelist = error;
        });
    },
    getStatusColor(status) {
      if (status === 'VALID') return 'green';
      if (status === 'EXPIRING SOON') return 'orange';
      if (status === 'NOT YET VALID') return 'orange';
      return 'red';
    },
    getManagedColor(status) {
      if (status === 'YES') return 'green';
      return 'red';
    },
  },
};
</script>
