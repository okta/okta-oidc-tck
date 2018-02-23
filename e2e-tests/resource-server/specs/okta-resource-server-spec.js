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

'use strict';
const commonUtils = require('../../tools/common-util');
const request = require('request');

describe('Okta Resource Server',  () => {
    const appRoot = `http://localhost:${browser.params.appPort}`;


    beforeAll(async () => {
        this.accessToken = await commonUtils.getAccessToken({
            ISSUER: process.env.ISSUER,
            CLIENT_ID: process.env.CLIENT_ID,
            REDIRECT_URI: process.env.REDIRECT_URI,
            USERNAME: process.env.USERNAME,
            PASSWORD: process.env.PASSWORD
        });
    });

    beforeEach( () => {
        browser.ignoreSynchronization = true;
    });

    it('requires an access token to get the /api/messages data', (done) => {

        request.get(appRoot + "/api/messages", {
            'auth': {
                'bearer': this.accessToken
            },
            'json': true
        }, function (error, response, body) {
            expect(body).not.toBe(null);
            expect(response.statusCode).toBe(200);
            expect(Object.keys(body)).toContain('messages');
            expect(body.messages).not.toBe(null);
            expect(Object.keys(body.messages[0])).toContain('date');
            expect(Object.keys(body.messages[0])).toContain('text');
            done();
        });
    });

    it('returns a 401 when trying to access the /api/messages route with an invalid token', (done) => {
        request.get(appRoot + "/api/messages", {
            'auth': {
                'bearer': 'invalid.access.token'
            },
            'json': true
        }, function (error, response, body) {
            expect(response.statusCode).toBe(401);

            done();
        });
    });

    it('returns a 401 when trying to access the /api/messages route without a token', (done) => {
        request.get(appRoot + "/api/messages", {
            'json': true
        }, function (error, response, body) {
            expect(response.statusCode).toBe(401);

            done();
        });
    });
});
