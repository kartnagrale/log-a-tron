import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({standalone:true,imports:[RouterLink],template:`
  <section class="access-denied panel">
    <span class="access-denied-icon"><i aria-hidden="true" class="pi pi-lock"></i></span>
    <p class="eyebrow">Authorization boundary</p>
    <h1>Access restricted</h1>
    <p>Your current role does not have access to this area. Choose an authorized workspace to continue.</p>
    <a class="button-link primary" routerLink="/overview"><i aria-hidden="true" class="pi pi-arrow-left"></i> Return to Overview</a><p class="help">Contact your project administrator if you need a different level of access.</p>
  </section>
`})
export class AccessDeniedComponent {}
