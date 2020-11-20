/*!
 * Copyright (c) 2015-2018, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License, Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 */

/* eslint no-console:0 */

const Monitor = require('forever-monitor').Monitor;
const waitOn = require('wait-on');
const chalk = require('chalk');
const find = require('find-process');
const platform = require('platform');
const { execSync } = require('child_process');

const daemonUtil = module.exports;

function startNpmScript(script, color, env) {
  const command = env ? `${env} npm run` : 'npm run'
  const child = new Monitor(script, {
    command,
    max: 1,
    silent: true,
    spawnWith: {shell: true}
  });

  const normal = chalk[color || 'yellow'];
  const bold = normal.bold.underline;

  child.on('stdout', (msg) => {
    console.log(normal(msg));
  });

  child.on('stderr', (msg) => {
    console.log(bold(`Failed to start "npm run ${script}"`));
    console.log(bold(msg));
  });

  child.on('exit', () => {
    console.log(normal(`Finished "npm run ${script}"`));

    if(platform.os.family !== 'Win32')
      return;

    // On windows the child processes aren't killed automatically
    if (child.args[0].includes('resource-server'))
      killProcessAtPort(8000);
    else
      killProcessAtPort(8080);
  });

  return new Promise((resolve, reject) => {
    child.on('start', () => {
      resolve(child);
    });
    console.log(normal(`Running "npm run ${script}"`));
    try {
      child.start();
    } catch (err) {
      reject(err);
    }
  });
}

function waitOnPromise(opts, context) {
  const normal = chalk[context.color || 'yellow'];
  const bold = normal.bold.underline;

  return new Promise((resolve, reject) => {
    const resourceJSON = JSON.stringify(opts.resources, null, 2);
    waitOn(opts, (err) => {
      if (err) {
        console.log(bold(`Failed while waiting for "npm run ${context.script}"`));
        console.log(bold(`Waiting for ${resourceJSON}`));
        console.log(bold(err));
        reject(err);
      } else {
        console.log(normal(`The following resources are available after "npm run ${context.script}":`));
        console.log(normal(resourceJSON));
        resolve();
      }
    });
  });
}

function startAndWait(opts) {
  return startNpmScript(opts.script, opts.color, opts.env)
  .then(child => waitOnPromise({
    resources: opts.resources,
  }, opts)
  .then(() => child));
}

async function killProcessAtPort(port) {
  // If server was started manually, we don't need to kill the server
  if (process.env.SERVER_STARTED === 'true') {
    return;
  }

  // For aspnet webforms samples, we use iisexpress that is started as system process
  // Using port to kill it doesn't work. Hence killing the process through name
  const iisexpress = await find('name', 'iisexpress.exe');

  if (iisexpress.length) {
    console.log('%s is running', iisexpress[0].name);
    execSync(`TASKKILL /F /IM ${iisexpress[0].name}`);
    console.log('Terminated the process %s', iisexpress[0].name);
    return;
  }

  const list = await find('port', port);

  if (list.length) {
    console.log('%s is listening port %s', list[0].name, port);
    execSync(`TASKKILL /F /PID ${list[0].pid}`);
    console.log('Terminated the process on port %s', port);
  }
}

daemonUtil.startOktaHostedLoginServer = () => startAndWait({
  script: 'okta-hosted-login-server',
  color: 'green',
  resources: [
    `tcp:8080`,
  ],
  env: 'BROWSER=none'
});

daemonUtil.startCustomLoginServer = () => startAndWait({
  script: 'custom-login-server',
  color: 'green',
  resources: [
    `tcp:8080`,
  ],
  env: 'BROWSER=none'
});

daemonUtil.startResourceServer = () => startAndWait({
  script: 'resource-server',
  color: 'green',
  resources: [
    `tcp:8000`,
  ]
});
