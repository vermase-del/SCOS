import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'scos-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="login-container">
      <div class="login-card glass-panel">
        <div class="login-header">
          <div class="login-logo">🏢</div>
          <h2>Welcome to SCOS</h2>
          <p>Smart Community Operating System</p>
        </div>

        <form (ngSubmit)="onLogin()" class="login-form">
          <div *ngIf="errorMessage()" class="alert-error">
            {{ errorMessage() }}
          </div>

          <div class="form-group">
            <label for="username">Username or Email</label>
            <input
              type="text"
              id="username"
              [(ngModel)]="username"
              name="username"
              class="form-control"
              placeholder="e.g. admin or resident@scos.local"
              required
            />
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input
              type="password"
              id="password"
              [(ngModel)]="password"
              name="password"
              class="form-control"
              placeholder="••••••••"
              required
            />
          </div>

          <button type="submit" class="btn-primary btn-block" [disabled]="loading()">
            {{ loading() ? 'Signing In...' : 'Sign In to Portal' }}
          </button>

          <div class="demo-credentials">
            <strong>Default Demo Credentials:</strong><br/>
            Username: <code>admin</code> | Password: <code>Password&#64;123</code>
          </div>

          <div class="register-cta">
            <span class="cta-label">New residential complex or housing society?</span>
            <a routerLink="/register-community" class="btn-register">
              🏛️ Onboard & Register Society
            </a>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: radial-gradient(circle at top right, #1e1b4b, #0f172a);
      padding: 1.5rem;
    }
    .login-card {
      width: 100%;
      max-width: 440px;
      padding: 2.5rem;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
    }
    .login-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .login-logo {
      font-size: 3rem;
      margin-bottom: 0.5rem;
    }
    .login-header h2 {
      font-size: 1.5rem;
      font-weight: 800;
      color: #ffffff;
      letter-spacing: -0.02em;
    }
    .login-header p {
      color: #94a3b8;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }
    .form-group {
      margin-bottom: 1.25rem;
    }
    .form-group label {
      display: block;
      font-size: 0.8125rem;
      font-weight: 600;
      color: #cbd5e1;
      margin-bottom: 0.5rem;
    }
    .btn-block {
      width: 100%;
      justify-content: center;
      padding: 0.875rem;
      margin-top: 0.75rem;
      font-size: 0.9375rem;
    }
    .alert-error {
      background: rgba(239, 68, 68, 0.2);
      border: 1px solid #ef4444;
      color: #fca5a5;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      font-size: 0.875rem;
      margin-bottom: 1.25rem;
    }
    .demo-credentials {
      margin-top: 1.5rem;
      padding: 0.75rem;
      background: rgba(255, 255, 255, 0.04);
      border: 1px dashed #334155;
      border-radius: 6px;
      font-size: 0.75rem;
      color: #94a3b8;
      text-align: center;
    }
    .demo-credentials code {
      color: #818cf8;
      font-weight: 600;
    }
    .register-cta {
      margin-top: 1.5rem;
      padding-top: 1.25rem;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }
    .cta-label {
      font-size: 0.8125rem;
      color: #94a3b8;
    }
    .btn-register {
      display: inline-block;
      width: 100%;
      text-align: center;
      padding: 0.65rem 1rem;
      border-radius: 8px;
      background: rgba(30, 41, 59, 0.7);
      border: 1px solid rgba(99, 102, 241, 0.35);
      color: #c7d2fe;
      text-decoration: none;
      font-size: 0.875rem;
      font-weight: 600;
      transition: all 0.2s ease;
      box-sizing: border-box;
    }
    .btn-register:hover {
      background: rgba(79, 70, 229, 0.25);
      border-color: #818cf8;
      color: #ffffff;
      transform: translateY(-1px);
    }
  `]
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  username = 'admin';
  password = 'Password@123';
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  onLogin(): void {
    if (!this.username?.trim() || !this.password?.trim()) {
      this.errorMessage.set('Please enter both username and password');
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Authentication failed. Please verify credentials.');
      }
    });
  }
}
