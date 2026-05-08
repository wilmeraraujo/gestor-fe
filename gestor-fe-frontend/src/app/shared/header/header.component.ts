import { Component, EventEmitter, Output } from '@angular/core';
import { UserMenuComponent } from '../components/user-menu/user-menu.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [UserMenuComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {

  @Output() toggle = new EventEmitter<void>();

  user:string="Prueba";
  email:string="prueba@gmail.com";

  toggleSidebar() {
    this.toggle.emit();
  }

}
