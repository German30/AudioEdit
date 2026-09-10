import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { IconComponent, IconName } from '../../shared/icon/icon.component';

interface NavItem {
  path: string;
  label: string;
  icon: IconName;
  admin?: boolean;
}

const NAV: NavItem[] = [
  { path: '/app/dashboard', label: 'Proyectos', icon: 'layers' },
  { path: '/app/assets', label: 'Biblioteca', icon: 'folder' },
  { path: '/app/subscription', label: 'Plan y uso', icon: 'gauge' },
  { path: '/app/settings', label: 'Ajustes', icon: 'settings' },
  { path: '/app/admin', label: 'Administración', icon: 'sliders', admin: true },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './app-shell.page.html',
  styleUrl: './app-shell.page.scss',
})
export class AppShellPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.user;
  protected readonly nav = computed(() => NAV.filter((n) => !n.admin || this.auth.isAdmin()));
  protected readonly initials = computed(() => {
    const name = this.user()?.displayName ?? this.user()?.email ?? '?';
    return name
      .split(/\s+/)
      .slice(0, 2)
      .map((p) => p.charAt(0).toUpperCase())
      .join('');
  });

  protected logout(): void {
    this.auth.logout().subscribe(() => this.router.navigateByUrl('/'));
  }
}
