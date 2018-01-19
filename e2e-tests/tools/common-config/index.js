/* global jasmine */
const jasmineReporters = require('jasmine-reporters');

var commonConfig = module.exports = {};

commonConfig.configure = function (promises) {
  const config = {
    // Set the following env vars to match your test environment
    // Note the USERNAME should be of the form "username@email.com"
    params: {
      login: {
        username: process.env.USERNAME,
        password: process.env.PASSWORD,
        email: process.env.USERNAME,
      },
      // App servers start on port 8080 but configurable using env var
      appPort: process.env.PORT || 8080,
      appTimeOut: process.env.TIMEOUT || 1000
    },
    framework: 'jasmine2',
    beforeLaunch() {
      return promises;
    },
    onPrepare() {
      jasmine.getEnv().addReporter(new jasmineReporters.JUnitXmlReporter({
        savePath: 'build2/reports/junit',
        filePrefix: 'results',
      }));
    },
    afterLaunch() {
      promises.then((childProcesses) => {
        childProcesses.forEach(child => child.stop());
      });
      return new Promise(resolve => setTimeout(() => resolve(), browser.params.appTimeOut));
    },
    specs: ['specs/*.js'],
    restartBrowserBetweenTests: false,
    capabilities: {
      browserName: 'chrome',
    }
  };

  if (process.env.SAUCE_USERNAME) {
    console.log('-- Using SauceLabs --');
    config.sauceUser = process.env.SAUCE_USERNAME;
    config.sauceKey = process.env.SAUCE_ACCESS_KEY;
    config.capabilities.tunnelIdentifier = process.env.TRAVIS_JOB_NUMBER;
    config.capabilities.build = process.env.TRAVIS_BUILD_NUMBER;
    config.capabilities.screenResolution = '1600x1200';
  }
  // Run Chrome Headless
  else if (process.env.CHROME_HEADLESS || process.env.TRAVIS) {
    console.log('-- Using Chrome Headless --');
    config.capabilities.chromeOptions = {
      args: ['--headless','--disable-gpu','--window-size=1600x1200']
    }
  }
  // otherwise just launch the browser locally
  else {
    console.log('-- Using Chrome --');
  }
  return config;
};
