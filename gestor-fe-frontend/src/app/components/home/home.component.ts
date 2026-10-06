import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoginService } from '../../services/login.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  private loginService = inject(LoginService);

  username: string = '';
  userRole: string = '';
  isAdmin: boolean = false;
  isPrestador: boolean = false;
  fechaActual: Date = new Date();

  ngOnInit(): void {
    this.loginService.getUserRoles();
    this.username = this.loginService.getUserName();
    this.userRole = this.loginService.userRole;
    this.isAdmin = this.loginService.isAdmin || this.loginService.isGAdmin;
    this.isPrestador = this.loginService.isPrestador;
  }
}
