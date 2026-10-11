/** Wait for the entire server window and local tile preparation, excluding cleanup. */
export class WorldLoadGate {
  private hidden = true;
  private transferComplete = false;

  setTransferState(complete: boolean, hideWorld: boolean): void {
    this.transferComplete = complete;
    if (hideWorld) this.hidden = true;
  }

  update(hasPendingTiles: boolean): boolean {
    if (this.transferComplete && !hasPendingTiles) this.hidden = false;
    return this.hidden;
  }
}
