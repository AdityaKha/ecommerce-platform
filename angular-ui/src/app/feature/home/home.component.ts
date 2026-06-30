import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCardModule, MatButtonModule],
  template: `
    <div class="home-container">
      <mat-card class="home-card">
        <mat-card-header>
          <mat-card-title>Welcome, {{ username }}!</mat-card-title>
          <mat-card-subtitle>You are signed in.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-actions>
          <button mat-stroked-button color="warn" (click)="logout()">Sign Out</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .home-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: #f5f5f5;
    }
    .home-card {
      width: 100%;
      max-width: 480px;
      padding: 16px;
    }
  `],
})
export class HomeComponent {
  username = this.authService.getUsername() ?? 'User';

  constructor(private authService: AuthService, private router: Router) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
