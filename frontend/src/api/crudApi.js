import axiosClient from './axiosClient';

/** Generic CRUD service factory for `GET ?params`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` */
export function createCrudApi(path) {
  return {
    list: (params) => axiosClient.get(path, { params }).then((r) => r.data),
    get: (id) => axiosClient.get(`${path}/${id}`).then((r) => r.data),
    create: (body) => axiosClient.post(path, body).then((r) => r.data),
    update: (id, body) => axiosClient.put(`${path}/${id}`, body).then((r) => r.data),
    remove: (id) => axiosClient.delete(`${path}/${id}`).then((r) => r.data),
  };
}
