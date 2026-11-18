export type CheckRecordType = 'ENTRADA' | 'SALIDA';
export type DailyRecordType = 'COMIDA' | 'PASEO' | 'JUEGO' | 'DESCANSO' | 'OTRO';
export type IncidentSeverity = 'LEVE' | 'MODERADA' | 'GRAVE';
export type ReservationModality = 'DIA_COMPLETO' | 'MEDIA_JORNADA' | 'MANANA' | 'TARDE';
export type ReservationBookingModality = 'DIA_COMPLETO' | 'MANANA' | 'TARDE';

export interface Allergy {
  id: number;
  description: string;
}

export interface Medication {
  id: number;
  medication: string;
  dosage: string | null;
  instructions: string;
}

export interface Incompatibility {
  id: number;
  incompatibleDogId: number;
  incompatibleDogName: string;
  reason: string | null;
}

export interface DailyDog {
  id: number;
  name: string;
  breed: string | null;
  size: 'PEQUENO' | 'MEDIANO' | 'GRANDE';
  birthDate: string | null;
  allergies: Allergy[];
  medications: Medication[];
  incompatibilities: Incompatibility[];
}

export interface GroupOccupancy {
  groupId: number;
  occupied: number;
  capacityMax: number;
}

export interface DailyReservation {
  id: number;
  date: string;
  modality: ReservationModality;
  status: 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA';
  dog: DailyDog;
  groupOccupancy: GroupOccupancy | null;
}

export interface CheckRecordRequest {
  type: CheckRecordType;
  petCondition?: string;
  observations?: string;
}

export interface DailyRecordRequest {
  type: DailyRecordType;
  notes?: string;
}

export interface IncidentRequest {
  severity: IncidentSeverity;
  description: string;
}

export interface ReservationGroup {
  id: number | null;
  label: string;
  occupancy: GroupOccupancy | null;
  reservations: DailyReservation[];
}

export type ReceptionPresenceStatus =
  | 'SIN_ENTRADA_REGISTRADA'
  | 'EN_EL_CENTRO'
  | 'SALIDA_REGISTRADA'
  | 'REGISTRO_INCOHERENTE';

export interface ReceptionReservation {
  id: number;
  date: string;
  modality: ReservationModality;
  status: 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA';
  dogId: number;
  dogName: string;
  tutorName: string | null;
  presenceStatus: ReceptionPresenceStatus;
}

export interface ReceptionReservationPage {
  items: ReceptionReservation[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ReceptionDog {
  id: number;
  name: string;
  breed: string | null;
  size: 'PEQUENO' | 'MEDIANO' | 'GRANDE';
  birthDate: string | null;
}

export interface ReceptionTutor {
  name: string;
  phone: string;
}

export interface ReceptionPresence {
  status: ReceptionPresenceStatus;
  lastEntryAt: string | null;
  entryPetCondition: string | null;
  lastExitAt: string | null;
  inconsistent: boolean;
}

export interface ReceptionReservationDetail {
  id: number;
  date: string;
  modality: ReservationModality;
  status: 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA';
  dog: ReceptionDog;
  tutor: ReceptionTutor | null;
  presence: ReceptionPresence;
}

export interface ReceptionGroupOption {
  id: number;
  name: string;
  capacityPerSlot: number;
}

export interface ReceptionAvailability {
  date: string;
  groupId: number;
  groupName: string;
  capacityPerSlot: number;
  morningOccupied: number;
  morningRemaining: number;
  afternoonOccupied: number;
  afternoonRemaining: number;
}

export interface ReceptionReservationCreateRequest {
  dogId: number;
  groupId: number;
  date: string;
  modality: ReservationBookingModality;
}

export interface ReceptionReservationCreated {
  id: number;
  date: string;
  modality: ReservationBookingModality;
  status: 'CONFIRMADA';
  dogId: number;
  dogName: string;
  groupId: number;
  groupName: string;
}

export interface Tutor {
  id: number;
  firstName: string;
  lastName: string;
  nationalId: string;
  phone: string;
  email: string;
}

export interface TutorPage {
  items: Tutor[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TutorRequest {
  firstName: string;
  lastName: string;
  nationalId: string;
  phone: string;
  email: string;
  confirmMatches: boolean;
}

export interface TutorVersionedResponse {
  tutor: Tutor;
  etag: string;
}

export type DogSize = 'PEQUENO' | 'MEDIANO' | 'GRANDE';

export interface Dog {
  id: number;
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  tutorId: number | null;
  tutorName: string | null;
  tutorNationalId: string | null;
}

export interface DogPage {
  items: Dog[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DogCreateRequest {
  tutorId: number;
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  confirmMatches: boolean;
}

export interface DogUpdateRequest {
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  clearBreed: boolean;
  clearBirthDate: boolean;
  confirmMatches: boolean;
}

export interface DogVersionedResponse {
  dog: Dog;
  etag: string;
}
