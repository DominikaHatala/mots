import Client from 'client-oauth2';
import axios from 'axios';

import { AUTH_USER, UNAUTH_USER, AUTH_ERROR, FETCH_CHWS, CREATE_HEALTH_WORKER } from './types';

const BASE_URL = '/api';

const auth = new Client({
  clientId: 'trusted-client',
  clientSecret: 'secret',
  accessTokenUri: `${BASE_URL}/oauth/token`
});

export function signinUser({ username, password }, callback) {
  return function (dispatch) {
    auth.owner.getToken(username, password)
    .then(response => {
      dispatch({ type: AUTH_USER });
      localStorage.setItem('token', response.accessToken);
      callback();
    })
    .catch(() => {
      dispatch(authError('Bad Credentials'));
    });
  }
}

export function authError(error) {
  return {
    type: AUTH_ERROR,
    payload: error
  };
}

export function signoutUser() {
  localStorage.removeItem('token');

  return { type: UNAUTH_USER };
}

export function fetchChws() {

  const token = localStorage.getItem('token');
  const url = `${BASE_URL}/chw?access_token=${token}`;
  const request = axios.get(url);

  return {
    type: FETCH_CHWS,
    payload: request
  }
}

export function createHealthWorker(values, callback) {
  const token = localStorage.getItem('token');

  const request = axios.post(`${BASE_URL}/chw?access_token=${token}`, values);
  request.then(() => callback());

  return {
    type: CREATE_HEALTH_WORKER,
    payload: request
  };
}
