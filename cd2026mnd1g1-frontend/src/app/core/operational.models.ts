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
  declaredByTutor: boolean;
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

export interface DailyRecord {
  id: number;
  reservationId: number;
  type: DailyRecordType;
  dateTime: string;
  notes: string | null;
}

export interface CaregiverDailySummary {
  id: number;
  reservationId: number;
  text: string;
  status: 'PUBLICADO';
  publishedAt: string;
}

export interface Incident {
  id: number;
  reservationId: number;
  severity: IncidentSeverity;
  description: string;
  dateTime: string;
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

export type GroomingRequestOrigin = 'INTERNA' | 'B2B';
export type GroomingRequestStatus = 'SOLICITADA' | 'CONFIRMADA' | 'RECHAZADA' | 'COMPLETADA' | 'CANCELADA';
export type GroomingAppointmentStatus =
  | 'PENDIENTE_ASIGNACION'
  | 'SOLICITADA'
  | 'CONFIRMADA'
  | 'COMPLETADA'
  | 'CANCELADA';

export interface GroomingRequest {
  id: number;
  dogName: string;
  customerName: string | null;
  serviceNames: string[];
  requestedDate: string;
  requestedTime: string | null;
  status: GroomingRequestStatus;
  origin: GroomingRequestOrigin;
}

export interface GroomingRequestPage {
  items: GroomingRequest[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface GroomingRequestDetail extends GroomingRequest {
  notes: string | null;
}

export interface GroomingAppointment {
  id: number;
  dogName: string;
  customerName: string | null;
  serviceNames: string[];
  date: string;
  time: string;
  durationMinutes: number;
  location: 'EN_INSTALACIONES' | 'DESPLAZADO' | null;
  status: GroomingAppointmentStatus;
  origin: GroomingRequestOrigin;
}

export interface GroomingAppointmentPage {
  items: GroomingAppointment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface GroomingAppointmentDetail extends GroomingAppointment {
  notes: string | null;
}

export interface GroomerOption {
  id: number;
  name: string;
}

export interface GroomerAgenda {
  groomerId: number;
  groomerName: string;
  date: string;
  appointments: GroomingAppointment[];
}

export interface GroomingDogOption {
  id: number;
  name: string;
  tutorName: string | null;
}

export interface GroomingDogOptionPage {
  items: GroomingDogOption[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface GroomingServiceOption {
  id: number;
  name: string;
}

export interface CreateGroomingAppointmentRequest {
  dogId: number;
  serviceId: number;
  date: string;
  time: string;
  notes: string | null;
}

export interface ReceptionDog {
  id: number;
  name: string;
  breed: string | null;
  size: 'PEQUENO' | 'MEDIANO' | 'GRANDE';
  birthDate: string | null;
  temperament: string | null;
  feedingInstructions: string | null;
  informationDeclaredByTutor: boolean;
  feedingDeclaredByTutor: boolean;
  temperamentDeclaredByTutor: boolean;
  allergies: { description: string; declaredByTutor: boolean }[];
  medications: Medication[];
}

export interface ReceptionTutor {
  name: string;
  phone: string;
}

export interface ReceptionPresence {
  status: ReceptionPresenceStatus;
  lastEntryAt: string | null;
  entryPetCondition: string | null;
  entryObservations: string | null;
  entryResponsible: string | null;
  lastExitAt: string | null;
  exitResponsible: string | null;
  collectorType: 'TUTOR' | 'PERSONA_AUTORIZADA' | null;
  collectorName: string | null;
  exitObservations: string | null;
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

export interface ReceptionArrivalRequest {
  observations: string | null;
}

export interface ReceptionArrivalResult {
  id: number;
  reservationId: number;
  type: 'ENTRADA';
  dateTime: string;
  observations: string | null;
  reservationStatus: string;
}

export interface AuthorizedPerson {
  id: number;
  name: string;
  phone: string;
}

export interface ReceptionDepartureData {
  reservationId: number;
  date: string;
  modality: ReservationModality;
  reservationStatus: 'PENDIENTE' | 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA';
  dogName: string;
  tutorId: number | null;
  tutorName: string | null;
  entryAt: string | null;
  entryObservations: string | null;
  presenceStatus: ReceptionPresenceStatus;
  authorizedPersons: AuthorizedPerson[];
}

export interface ReceptionDepartureRequest {
  collectorType: 'TUTOR' | 'PERSONA_AUTORIZADA';
  authorizedPersonId: number | null;
  observations: string | null;
}

export interface ReceptionDepartureResult {
  id: number;
  reservationId: number;
  type: 'SALIDA';
  dateTime: string;
  observations: string | null;
  reservationStatus: string;
  collectorType: 'TUTOR' | 'PERSONA_AUTORIZADA';
  collectorName: string;
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
  feedingInstructions: string | null;
  temperament: string | null;
  tutorId: number | null;
  tutorName: string | null;
  tutorNationalId: string | null;
}

export interface DogDetail {
  dog: Dog;
  authorizedPersons: AuthorizedPerson[];
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
  feedingInstructions: string | null;
  temperament: string | null;
  confirmMatches: boolean;
}

export interface DogUpdateRequest {
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  feedingInstructions: string | null;
  temperament: string | null;
  clearBreed: boolean;
  clearBirthDate: boolean;
  clearFeedingInstructions: boolean;
  clearTemperament: boolean;
  confirmMatches: boolean;
}

export interface DogVersionedResponse {
  dog: Dog;
  etag: string;
}

export interface DogDetailVersionedResponse {
  dog: Dog;
  authorizedPersons: AuthorizedPerson[];
  etag: string;
}

export interface CaregiverDogSummary {
  reservationId: number;
  id: number;
  name: string;
  breed: string | null;
  size: DogSize;
  photoAvailable: boolean;
  arrivalAt: string | null;
  stayStatus: string;
  groupName: string | null;
}

export interface CaregiverDogPage {
  items: CaregiverDogSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CaregiverDogDetail {
  reservationId: number;
  id: number;
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  tutor: { name: string; phone: string } | null;
  photoAvailable: boolean;
  arrivalAt: string | null;
  stayStatus: string;
  groupName: string | null;
  feedingInstructions: string | null;
  temperament: string | null;
  allergies: { description: string }[];
  medications: { id: number; medication: string; dosage: string | null; instructions: string; declaredByTutor: boolean }[];
  incompatibilities: { incompatibleDogName: string; reason: string | null }[];
}

export interface TutorDogSummary {
  id: number;
  name: string;
  breed: string | null;
  size: DogSize;
  photoAvailable: boolean;
}

export interface TutorDogPage {
  items: TutorDogSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TutorMedication {
  medication: string;
  dosage: string | null;
  instructions: string;
  declaredByTutor: boolean;
}

export interface TutorAuthorizedPerson {
  id: number;
  name: string;
  phone: string;
}

export interface TutorDogVersionedResponse {
  dog: TutorDogDetail;
  etag: string;
}

export interface TutorDogDetail {
  id: number;
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  photoAvailable: boolean;
  feedingInstructions: string | null;
  feedingDeclaredByTutor: boolean;
  allergies: { description: string; declaredByTutor: boolean }[];
  medications: TutorMedication[];
  authorizedPersons: TutorAuthorizedPerson[];
}

export interface TutorDogCreateRequest {
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  allergies: { description: string }[];
  medications: { medication: string; dosage: string | null; instructions: string }[];
  authorizedPersons: { name: string; phone: string }[];
}

export interface TutorDogUpdateRequest {
  name: string;
  breed: string | null;
  size: DogSize;
  birthDate: string | null;
  allergies: { description: string }[];
  medications: { medication: string; dosage: string | null; instructions: string }[];
  authorizedPersons: { id: number | null; name: string; phone: string }[];
  removePhoto: boolean;
}
