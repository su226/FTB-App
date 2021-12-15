import { ModalBox } from '@/types';

export interface SocketState {
  firstStart: boolean;
  socket: Socket;
  messages: { [index: string]: (data: any, wsMessageId?: number) => void };
  modal: ModalBox | null;
  reconnects: number;
  ircEventCallback: ((data: any) => void) | undefined;
  pingEventCallback: ((data: any) => void) | undefined;
  modProgressCallback: ((data: any) => void) | undefined;
  launchProgressCallback: ((data: any) => void) | undefined;
  exitCallback: ((data: any) => void) | undefined;
  downloadedFiles: { [index: string]: string };
}

export interface Socket {
  isConnected: boolean;
  message: string;
  reconnectError: boolean;
}
