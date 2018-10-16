<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
    <h2>Stats: <code>{{ JSON.stringify(stats) }}</code></h2>
    <div>
      <button-spinner
        :is-loading="isSubmitLoading" 
        :disabled="isSubmitLoading"
        :status="submitStatus"
        v-on:click.native="clickme">
        <span>submit</span>
      </button-spinner>
      <input type="text" class="ml-2" v-model="reqUrl" />
    </div>
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
          <td><code><a :href="`http://localhost:8080/api/getfile/${r.contentFileNum}`">{{ r.contentFileNum }}</a></code></td>
        </tr>
      </table>
    </div>
  </div>
</template>

<script>
import VueButtonSpinner from 'vue-button-spinner'
import _ from 'lodash'

export default {
  name: 'HelloWorld',
  data () {
    return {
      reqUrl: 'https://api.ipify.org/?format=json',
      msg: 'Hashgraph Apollo VPN Monitor',
      stats: {},
      reqs: [],
      resps: [],
      submitStatus: '',
      isSubmitLoading: false,
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
          r.contentFileNum = _.get(_.find(this.resps, { id: r.id }), 'contentFileNum')
          return r
        })
    }
  },
  components: {	
    'button-spinner': VueButtonSpinner
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

      this.refreshTimeout = setTimeout(() => this.refreshRequests(), 2000)
    },
    clickme () {
      this.isSubmitLoading = true
      fetch(`/api/postreq/${btoa(this.reqUrl)}`)
        .then(res => res.json())
        .then(res => {
          this.isSubmitLoading = false
          this.submitStatus = true
          setTimeout(() => {
            this.isSubmitLoading = false
            this.submitStatus = ''    
          }, 1000)
        })
        .catch(err => {
          this.isSubmitLoading = false
          this.submitStatus = false
          setTimeout(() => {
            this.isSubmitLoading = false
            this.submitStatus = ''
          }, 2000)
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
