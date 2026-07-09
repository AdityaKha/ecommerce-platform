import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="auth-container">
      <div class="auth-brand">
        <span class="brand-icon">🛍️</span>
        <h1>Emporia</h1>
        <p>Your one-stop shop for everyday essentials</p>
      </div>
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>Sign In</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username" />
              @if (form.get('username')?.hasError('required') && form.get('username')?.touched) {
                <mat-error>Username is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password" />
              @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
                <mat-error>Password is required</mat-error>
              }
            </mat-form-field>

            @if (errorMessage) {
              <p class="error-message">{{ errorMessage }}</p>
            }

            <button
              mat-flat-button
              color="primary"
              type="submit"
              class="full-width submit-btn"
              [disabled]="loading"
            >
              @if (loading) {
                <mat-spinner diameter="20" />
              } @else {
                Sign In
              }
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions>
          <p class="auth-link">
            Don't have an account? <a routerLink="/auth/register">Register</a>
          </p>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      gap: 24px;
      min-height: 100vh;
      padding: 24px 16px;
      background: linear-gradient(150deg, #2c4fa3, #3f6bd8 55%, #7b5cd6);
    }
    .auth-brand {
      text-align: center;
      color: #fff;
    }
    .auth-brand .brand-icon {
      font-size: 40px;
    }
    .auth-brand h1 {
      margin: 4px 0;
      font-size: 30px;
      letter-spacing: 0.5px;
    }
    .auth-brand p {
      margin: 0;
      opacity: 0.85;
      font-size: 14px;
    }
    .auth-card {
      width: 100%;
      max-width: 400px;
      padding: 16px;
      border-radius: 14px;
    }
    .full-width {
      width: 100%;
    }
    .submit-btn {
      margin-top: 8px;
    }
    .error-message {
      color: #f44336;
      font-size: 14px;
      margin: 4px 0 8px;
    }
    .auth-link {
      text-align: center;
      font-size: 14px;
      margin: 0;
    }
    mat-card-content form {
      display: flex;
      flex-direction: column;
    }
  `],
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  loading = false;
  errorMessage = '';

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.authService.login(this.form.getRawValue()).subscribe({
      next: (res) => {
        console.log(res)
        this.authService.saveSession(res);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message ?? 'Login failed. Please check your credentials.';
      },
    });
  }
}
