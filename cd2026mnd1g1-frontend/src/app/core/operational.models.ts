export type CheckRecordType = 'ENTRADA' | 'SALIDA';
export type DailyRecordType = 'COMIDA' | 'PASEO' | 'JUEGO' | 'DESCANSO' | 'OTRO';
export type IncidentSeverity = 'LEVE' | 'MODERADA' | 'GRAVE';

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
  modality: 'DIA_COMPLETO' | 'MEDIA_JORNADA';
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
