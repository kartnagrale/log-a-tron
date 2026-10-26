import { Component, inject } from '@angular/core';import { ThemeService } from './core/theme/theme.service';import { RouterOutlet } from '@angular/router';
@Component({selector:'app-root',standalone:true,imports:[RouterOutlet],template:'<router-outlet />'}) export class AppComponent{private readonly theme=inject(ThemeService);}

