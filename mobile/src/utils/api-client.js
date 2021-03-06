import _ from 'lodash';
import { Actions } from 'react-native-router-flux';
import { AsyncStorage } from 'react-native';
import RNFetchBlob from 'react-native-fetch-blob';

import Config from '../../config';
import { signoutUser, useRefreshToken } from '../actions';
import { dispatch } from '../App';
import { FETCH_ERROR } from '../actions/types';

const CLIENT_URL = Config.api[Config.backend.instance];
const VALID_STATUSES = [200, 201];

const getErrorMessage = (errorResponse) => {
  if (errorResponse) {
    return _.values(errorResponse).join('\n');
  }

  return '';
};

const getAlert = (title, message) => {
  Actions.modalError({
    message, title,
  });
};

export default class ApiClient {
  static async handleError(error) {
    const refreshToken = await AsyncStorage.getItem('refresh_token');

    switch (error.status) {
      case 401:
        if (refreshToken) {
          return dispatch(useRefreshToken(refreshToken));
        }
        return dispatch(signoutUser());
      case 403:
        getAlert('Access denied.', 'You don\'t have permissions to fetch this data');
        break;
      case 404:
        getAlert('Resource not found.', 'There were some problems with data fetching.');
        break;
      case 500:
        error.json().then((data) => {
          const errorMessage = data.message ? data.message : getErrorMessage(data);
          const message = errorMessage || data;
          getAlert('Error occurred.', message);
        }).catch(() =>
          getAlert('Error occurred.', 'There was an internal server error.'));
        break;
      default: {
        error.json().then((data) => {
          const errorMessage = getErrorMessage(data);
          const message = errorMessage || data;
          getAlert('Error occurred.', message);
        }).catch(err => getAlert('Error occurred.', err));
      }
    }
    return error;
  }

  static async chooseMethod(method, route, type, body) {
    return ApiClient.motsFetch({
      method,
      url: route,
      body: JSON.stringify(body) || undefined,
    })
      .then((response) => {
        if (VALID_STATUSES.indexOf(response.status) !== -1) {
          if (type === 'text') {
            return response.text()
              .then(responseText => responseText)
              .catch(() => {});
          }
          return response.json()
            .then(responseJson => responseJson)
            .catch(() => {});
        }
        throw response;
      })
      .catch((obj) => {
        dispatch({
          type: FETCH_ERROR,
          payload: true,
        });
        ApiClient.handleError(obj);
      });
  }

  static async get(route) {
    return ApiClient.chooseMethod('GET', route, 'json');
  }

  static async getText(route) {
    return ApiClient.chooseMethod('GET', route, 'text');
  }

  static async post(route, body) {
    return ApiClient.chooseMethod('POST', route, 'json', body);
  }

  static async put(route, body) {
    return ApiClient.chooseMethod('PUT', route, 'json', body);
  }

  static async delete(route) {
    return ApiClient.chooseMethod('DELETE', route, 'json');
  }

  static async send(effect) {
    return ApiClient.chooseMethod(effect.method, effect.url, 'json', effect.body);
  }

  static async motsFetch(_options) {
    dispatch({
      type: FETCH_ERROR,
      payload: false,
    });
    const fetchOptions = _.extend({
      headers: {},
      method: 'GET',
      url: null,
    }, _options);

    const token = await AsyncStorage.getItem('token');
    fetchOptions.headers.Authorization = `Bearer ${token}`;
    if (['POST', 'PUT', 'DELETE'].indexOf(fetchOptions.method) !== -1) {
      fetchOptions.headers.Accept = 'application/json';
      fetchOptions.headers['Content-Type'] = 'application/json';
    }

    return fetch(CLIENT_URL + fetchOptions.url, fetchOptions);
  }

  static async downloadReport(url, fileName, fileExtention) {
    const token = await AsyncStorage.getItem('token');
    const mimeType = fileExtention === 'xls' ? 'application/vnd.ms-excel' : 'application/pdf';
    return RNFetchBlob
      .config({
        fileCache: true,
        path: `${RNFetchBlob.fs.dirs.DownloadDir}/${fileName.replace(/ /g, '_')}.${fileExtention}`,
      })
      .fetch('GET', CLIENT_URL + url, {
        Authorization: `Bearer ${token}`,
      })
      .then((res) => {
        RNFetchBlob.android.actionViewIntent(res.path(), mimeType);
      });
  }
}
