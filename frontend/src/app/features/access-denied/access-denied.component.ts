import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({standalone:true,imports:[RouterLink],template:`
  <section class="access-denied panel">
    <span class="access-denied-icon"><i class="pi pi-lock"></i></span>
    <p class="eyebrow">Authorization boundary</p>
    <h1>Access restricted</h1>
    <p>Your current role does not have access to this area.</p>
    <a class="button-link primary" routerLink="/overview"><i class="pi pi-arrow-left"></i> Return to Overview</a>
  </section>
`})
export class AccessDeniedComponent {}
