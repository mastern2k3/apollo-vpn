<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
    <h2>Stats: <code>{{ JSON.stringify(stats) }}</code></h2>
    <div>
      <button v-on:click="clickme">Clickme</button>
      <input type="text" v-model="reqUrl" /></div>
    <div class="mt-2">
      <table style="width: auto" class="mx-auto table">
        <tr>
          <td>id</td>
          <td>url</td>
          <td>response</td>
        </tr>
        <tr v-for="r in getReqs" v-bind:key="r.id">
          <td><code>{{ r.id }}</code></td>
          <td><code>{{ r.uri }}</code></td>
          <td><code>{{ r.response }}</code></td>
        </tr>
      </table>
    </div>
  </div>
</template>

<script>
const _ = require('lodash')

export default {
  name: 'HelloWorld',
  data () {
    return {
      reqUrl: 'https://api.ipify.org/?format=json',
      msg: 'Hashgraph Apollo VPN Monitor',
      stats: {},
      reqs: [],
      resps: [],
    }
  },
  mounted () {
    fetch(`/api/stats`)
      .then(res => res.json())
      .then(res => {
        this.stats = res
        this.refreshRequests()
      })
      .catch(err => {
        this.stats = err
      })
  },
  computed: {
    getReqs () {
      return _.cloneDeep(this.reqs)
      .map(r => {
          r.response = _.get(_.find(this.resps, { id: r.id }), 'content')
          return r
        })
    }
  },
  methods: {
    refreshRequests () {
      fetch(`/api/getfile/${this.stats.reqFileNum}`)
        .then(res => res.text())
        .then(txt => {
          if (!txt) return
          this.reqs = txt.split('\n').filter(ln => ln).map(ln => JSON.parse(ln))
        })

      fetch(`/api/getfile/${this.stats.resFileNum}`)
        .then(res => res.text())
        .then(txt => {
          if (!txt) return
          this.resps = txt.split('\n').filter(ln => ln).map(ln => JSON.parse(ln))
        })
      
      setTimeout(() => this.refreshRequests(), 2000)
    },
    clickme () {
      fetch(`/api/postreq/${btoa(this.reqUrl)}`)
        .then(res => res.json())
        .then(res => {
          alert(JSON.stringify(res))
        })
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1, h2 {
  font-weight: normal;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
code {
  color: #6f42c1;
}
</style>
