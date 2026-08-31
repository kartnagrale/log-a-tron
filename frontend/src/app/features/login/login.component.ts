import { Component,inject,OnInit,signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { AuthService } from '../../core/auth/auth.service';
import { roleLabel } from '../../core/auth/capability.service';

interface IdentityOption {label:string;value:string;role:string}
const identityRoles:Record<string,string>={admin:'ADMIN','project-admin':'PROJECT_ADMIN',support:'PRODUCTION_SUPPORT',developer:'DEVELOPER',viewer:'VIEWER',auditor:'AUDITOR'};
const identityNames:Record<string,string>={admin:'Admin Demo','project-admin':'Project Admin Demo',support:'Support Demo',developer:'Developer Demo',viewer:'Viewer Demo',auditor:'Auditor Demo'};

@Component({standalone:true,imports:[FormsModule,ButtonModule,CardModule,SelectModule,MessageModule],template:`
<main class="login-page app-dark"><section class="login-hero"><div class="brand-mark large">L</div><p class="eyebrow">OBSERVABILITY CONTROL PLANE</p><h1>Find the first failure.<br><span>Follow every signal.</span></h1><p>LOG-A-TRON centralizes a governed service catalog with secure cross-service telemetry search and deterministic investigation.</p></section>
<p-card header="Development sign in" subheader="Choose a local demo identity to preview its real authorized scope."><label for="identity">Demo identity</label><p-select inputId="identity" [options]="users()" optionLabel="label" optionValue="value" [(ngModel)]="username" placeholder="Choose a demo identity" [fluid]="true"><ng-template #item let-option><div class="identity-option"><strong>{{option.label}}</strong><small>{{roleDisplay(option.role)}} · {{option.role}}</small></div></ng-template><ng-template #selectedItem let-option><div class="identity-option"><strong>{{option.label}}</strong><small>{{roleDisplay(option.role)}} · {{option.role}}</small></div></ng-template></p-select><p-message severity="warn" size="small">Local dev-auth only. Never enable this issuer in production.</p-message>@if(error()){<p-message severity="error">{{error()}}</p-message>}<p-button label="Issue signed development token" icon="pi pi-shield" [loading]="loading()" [disabled]="!username" [fluid]="true" (onClick)="login()" /></p-card></main>`})
export class LoginComponent implements OnInit {
 private readonly auth=inject(AuthService);private readonly router=inject(Router);readonly users=signal<IdentityOption[]>([]);username:string|null=null;loading=signal(false);error=signal('');
 ngOnInit(){this.auth.demoUsers().subscribe({next:users=>this.users.set(users.map(value=>({value,role:identityRoles[value]??'UNASSIGNED',label:identityNames[value]??value}))),error:()=>this.error.set('Demo identities are unavailable. Confirm the API is running with dev-auth.')});}
 roleDisplay(role:string){return roleLabel(role);}login(){if(!this.username)return;this.loading.set(true);this.error.set('');this.auth.login(this.username).subscribe({next:()=>void this.router.navigate(['/overview']),error:()=>{this.error.set('Sign-in failed. Confirm the selected dev identity is provisioned.');this.loading.set(false);}});}
}
