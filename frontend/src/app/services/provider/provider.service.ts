import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {Provider} from 'src/app/providers/provider/model/provider';
import {environment} from '../../../environments/environment';

const API: string = environment.serverUrl;
const PROVIDER_API = API + '/providers';

@Injectable({providedIn: 'root'})
export class ProviderService {

  httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) {}

  /** GET all providers **/
  getAllProviders(): Observable<Provider[]> {
    return this.http.get<Provider[]>(PROVIDER_API + '/findAll');
  }

  /** GET provider by id **/
  getProvider(id: string): Observable<any> {
    return this.http.get<any>(PROVIDER_API + '/' + id);
  }

  getAllGenresFromProvider(id: string): Observable<any> {
    return this.http.get<any[]>(PROVIDER_API + '/' + id + '/genres');
  }

  createProvider(provider: object): Observable<object> {
    return this.http.post(PROVIDER_API + '/add', provider);
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {

      // TODO: send the error to remote logging infrastructure
      console.error(error); // log to console instead

      // TODO: better job of transforming error for user consumption
      this.log(`${operation} failed: ${error.message}`);

      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }


  /** Log a ProviderService message with the MessageService */
  private log(message: string) {
    // TODO
  }

}
