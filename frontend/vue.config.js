module.exports = {

  chainWebpack: config => {
    config.externals({
      'my-app-settings': 'myAppSettings'
    })
  },

  publicPath: process.env.NODE_ENV === 'production'
    ? '/myproject/'
    : '/',

  lintOnSave: false,
  transpileDependencies: [
    'vuetify',
  ],
};
