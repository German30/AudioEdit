import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { PlansApi } from '../../core/api/plans-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { Plan } from '../../core/models';
import { formatBytes, formatLimit, formatPrice } from '../../core/util/format';
import { IconComponent } from '../../shared/icon/icon.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';
import { WaveformComponent } from '../../shared/waveform/waveform.component';

interface Step {
  n: string;
  title: string;
  body: string;
}

interface PlanLine {
  label: string;
  value: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, SpinnerComponent, WaveformComponent],
  templateUrl: './landing.page.html',
  styleUrl: './landing.page.scss',
})
export class LandingPage {
  private readonly plansApi = inject(PlansApi);
  private readonly auth = inject(AuthService);

  protected readonly year = new Date().getFullYear();
  protected readonly ctaTarget = computed(() => (this.auth.isAuthenticated() ? '/app' : '/login'));
  protected readonly ctaLabel = computed(() => (this.auth.isAuthenticated() ? 'Abrir el estudio' : 'Empezar gratis'));

  protected readonly plans = signal<Plan[]>([]);
  protected readonly plansState = signal<'loading' | 'ready' | 'error'>('loading');

  protected readonly steps: Step[] = [
    { n: '01', title: 'Sube tus pistas', body: 'WAV, MP3 o FLAC directo a almacenamiento con URL firmada. Auralis analiza duración, sample rate y forma de onda al vuelo.' },
    { n: '02', title: 'Edita sin destruir', body: 'Recortes, fundidos, velocidad, reversa y cadenas de efectos por clip, pista y master. El archivo original nunca se toca.' },
    { n: '03', title: 'Renderiza en la nube', body: 'Cada export congela una versión del grafo y se compila en un solo pase de ffmpeg. Sigues el progreso del job en tiempo real.' },
    { n: '04', title: 'Exporta y comparte', body: 'Mezcla final y stems por pista con enlaces de descarga temporales. Invita colaboradores con permisos por proyecto.' },
  ];

  protected readonly engine: string[] = [
    'atrim · areverse · atempo y cadenas de efectos compiladas a un único -filter_complex',
    'Instantáneas de cada arreglo con restauración a un clic',
    'Colaboradores por proyecto con roles VIEWER, EDITOR y OWNER',
    'Cuotas de render, almacenamiento y bitrate aplicadas por plan',
  ];

  constructor() {
    this.plansApi
      .list()
      .pipe(catchError(() => of(null)))
      .subscribe((plans) => {
        if (!plans) {
          this.plansState.set('error');
          return;
        }
        this.plans.set([...plans].sort((a, b) => a.priceCents - b.priceCents));
        this.plansState.set('ready');
      });
  }

  protected price(plan: Plan): string {
    return formatPrice(plan.priceCents, plan.currency);
  }

  protected isFeatured(plan: Plan): boolean {
    return plan.tier === 'PRO';
  }

  protected lines(plan: Plan): PlanLine[] {
    const e = plan.entitlements;
    return [
      { label: 'Proyectos', value: formatLimit(e.maxProjects) },
      { label: 'Render al mes', value: e.monthlyRenderMinutes < 0 ? 'Ilimitado' : `${e.monthlyRenderMinutes} min` },
      { label: 'Almacenamiento', value: formatBytes(e.maxStorageBytes, 0) },
      { label: 'Subida máx.', value: formatBytes(e.maxUploadBytes, 0) },
      { label: 'Formatos', value: e.allowedExportFormats.map((f) => f.toUpperCase()).join(' · ') },
      { label: 'Stems por pista', value: e.allowStemExport ? 'Sí' : '—' },
      { label: 'Bitrate máx.', value: `${e.maxExportBitrateKbps} kbps` },
      { label: 'Historial', value: `${e.historyRetentionDays} días` },
    ];
  }
}
