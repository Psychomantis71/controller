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
export default {
  data() {
    return {
      selected: [],
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
        .put('http://localhost:8091/api/instance/adopt', this.selected)
        .then((response) => {
          console.log('Get response: ', response.data);
          this.getInstanceData();
        })
        .catch((error) => {
          this.alert = true;
          console.log('Error while adopting: ', error);
        });
    },
  },
};
</script>
