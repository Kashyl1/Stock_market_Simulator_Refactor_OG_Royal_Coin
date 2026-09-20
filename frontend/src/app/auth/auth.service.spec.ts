import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { UserStatus } from '../core/user-status';
import { TestUsers } from '../testing/test-users';
import { AUTH_PATH, AuthService, REGISTER_PATH, RegisterResponse } from './auth.service';

const REGISTER_URL = `${environment.apiUrl}${AUTH_PATH}${REGISTER_PATH}`;
const POST_METHOD = 'POST';
const USER_ID = 7;
const CREATED: RegisterResponse = { userId: USER_ID, status: UserStatus.PendingVerification };

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('posts the registration to the auth endpoint and returns the new account', () => {
    const request = {
      email: TestUsers.email,
      displayName: TestUsers.displayName,
      password: TestUsers.password,
    };
    let response: RegisterResponse | undefined;

    service.register(request).subscribe((result) => (response = result));

    const call = httpMock.expectOne(REGISTER_URL);
    expect(call.request.method).toBe(POST_METHOD);
    expect(call.request.body).toEqual(request);
    call.flush(CREATED);

    expect(response).toEqual(CREATED);
  });
});
