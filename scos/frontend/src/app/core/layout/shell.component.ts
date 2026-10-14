import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './header.component';
import { SidebarComponent } from './sidebar.component';

@Component({
  selector: 'scos-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HeaderComponent, SidebarComponent],
  template: `
    <div class="app-layout">
      <scos-header></scos-header>
      <div class="content-wrapper">
        <scos-sidebar></scos-sidebar>
        <main class="main-content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .app-layout {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    .content-wrapper {
      display: flex;
      flex: 1;
    }
    .main-content {
      flex: 1;
      padding: 2rem;
      overflow-y: auto;
      max-height: calc(100vh - 70px);
    }
  `]
})
export class ShellComponent {}
