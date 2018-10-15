<template>
  <div class="hello">
    <h1>{{ msg }}</h1>
    <h2>Stats: <code>{{ JSON.stringify(stats) }}</code></h2>
    <button v-on:click="clickme">Clickme</button>
    <input type="text" v-model="reqUrl" />
  </div>
</template>

<script>
export default {
  name: 'HelloWorld',
  data () {
    return {
      reqUrl: 'https://api.ipify.org/?format=json',
      msg: 'Hashgraph Apollo VPN Monitor',
      stats: {}
    }
  },
  mounted () {
    fetch(`/api/stats`)
      .then(res => res.json())
      .then(res => {
        this.stats = res
      })
      .catch(err => {
        this.stats = err
      })
  },
  methods: {
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
a {
  color: #42b983;
}
</style>
