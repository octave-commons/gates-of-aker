import type { Snapshot, Agent, Tile, Stockpile, Job } from '../../../types';

export interface ValidationError {
  field: string;
  message: string;
  severity: 'error' | 'warning';
}

export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationError[];
}

export class StateValidator {
  static validateSnapshot(snapshot: Snapshot): ValidationResult {
    const errors: ValidationError[] = [];
    const warnings: ValidationError[] = [];

    // Basic structure validation
    if (typeof snapshot.tick !== 'number' || snapshot.tick < 0) {
      errors.push({
        field: 'tick',
        message: 'Tick must be a non-negative number',
        severity: 'error'
      });
    }

    // Agent validation
    if (Array.isArray(snapshot.agents)) {
      snapshot.agents.forEach((agent, index) => {
        const agentErrors = this.validateAgent(agent, index);
        errors.push(...agentErrors);
      });
    } else if (snapshot.agents !== undefined) {
      errors.push({
        field: 'agents',
        message: 'Agents must be an array',
        severity: 'error'
      });
    }

    // Tile validation
    if (snapshot.tiles && typeof snapshot.tiles === 'object') {
      const tileErrors = this.validateTiles(snapshot.tiles);
      errors.push(...tileErrors);
    }

    // Stockpile validation
    if (snapshot.stockpiles && typeof snapshot.stockpiles === 'object') {
      const stockpileErrors = this.validateStockpiles(snapshot.stockpiles);
      errors.push(...stockpileErrors);
    }

    // Job validation
    if (Array.isArray(snapshot.jobs)) {
      snapshot.jobs.forEach((job, index) => {
        const jobErrors = this.validateJob(job, index);
        errors.push(...jobErrors);
      });
    } else if (snapshot.jobs !== undefined) {
      errors.push({
        field: 'jobs',
        message: 'Jobs must be an array',
        severity: 'error'
      });
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings
    };
  }

  static validateAgent(agent: Agent, index: number): ValidationError[] {
    const errors: ValidationError[] = [];

    if (!agent.id) {
      errors.push({
        field: `agents[${index}].id`,
        message: 'Agent must have an ID',
        severity: 'error'
      });
    }

    if (agent.pos && !Array.isArray(agent.pos)) {
      errors.push({
        field: `agents[${index}].pos`,
        message: 'Agent position must be an array',
        severity: 'error'
      });
    }

    if (agent.pos && Array.isArray(agent.pos) && agent.pos.length !== 2) {
      errors.push({
        field: `agents[${index}].pos`,
        message: 'Agent position must have exactly 2 coordinates',
        severity: 'error'
      });
    }

    if (agent.needs && typeof agent.needs !== 'object') {
      errors.push({
        field: `agents[${index}].needs`,
        message: 'Agent needs must be an object',
        severity: 'error'
      });
    }

    if (agent.needs) {
      Object.entries(agent.needs).forEach(([need, value]) => {
        if (typeof value !== 'number' || value < 0 || value > 1) {
          errors.push({
            field: `agents[${index}].needs.${need}`,
            message: 'Need values must be numbers between 0 and 1',
            severity: 'error'
          });
        }
      });
    }

    return errors;
  }

  static validateTiles(tiles: Record<string, Tile>): ValidationError[] {
    const errors: ValidationError[] = [];

    Object.entries(tiles).forEach(([key, tile]) => {
      // Validate tile key format (q,r coordinates)
      if (!/^-?\d+,-?\d+$/.test(key)) {
        errors.push({
          field: `tiles.${key}`,
          message: 'Tile key must be in format "q,r" with integer coordinates',
          severity: 'error'
        });
      }

      // Validate tile structure
      if (tile.resource && typeof tile.resource !== 'string') {
        errors.push({
          field: `tiles.${key}.resource`,
          message: 'Tile resource must be a string',
          severity: 'error'
        });
      }

      if (tile.structure && typeof tile.structure !== 'string') {
        errors.push({
          field: `tiles.${key}.structure`,
          message: 'Tile structure must be a string',
          severity: 'error'
        });
      }

      if (tile.terrain && typeof tile.terrain !== 'string') {
        errors.push({
          field: `tiles.${key}.terrain`,
          message: 'Tile terrain must be a string',
          severity: 'error'
        });
      }
    });

    return errors;
  }

  static validateStockpiles(stockpiles: Record<string, Stockpile>): ValidationError[] {
    const errors: ValidationError[] = [];

    Object.entries(stockpiles).forEach(([key, stockpile]) => {
      // Validate stockpile key format (q,r coordinates)
      if (!/^-?\d+,-?\d+$/.test(key)) {
        errors.push({
          field: `stockpiles.${key}`,
          message: 'Stockpile key must be in format "q,r" with integer coordinates',
          severity: 'error'
        });
      }

      // Validate quantities
      const currentQty = stockpile.currentQty ?? stockpile['current-qty'];
      const maxQty = stockpile.maxQty ?? stockpile['max-qty'];

      if (typeof currentQty === 'number' && currentQty < 0) {
        errors.push({
          field: `stockpiles.${key}.currentQty`,
          message: 'Current quantity cannot be negative',
          severity: 'error'
        });
      }

      if (typeof maxQty === 'number' && maxQty <= 0) {
        errors.push({
          field: `stockpiles.${key}.maxQty`,
          message: 'Max quantity must be positive',
          severity: 'error'
        });
      }

      if (typeof currentQty === 'number' && typeof maxQty === 'number' && currentQty > maxQty) {
        errors.push({
          field: `stockpiles.${key}`,
          message: 'Current quantity cannot exceed max quantity',
          severity: 'error'
        });
      }
    });

    return errors;
  }

  static validateJob(job: Job, index: number): ValidationError[] {
    const errors: ValidationError[] = [];

    if (!job.id) {
      errors.push({
        field: `jobs[${index}].id`,
        message: 'Job must have an ID',
        severity: 'error'
      });
    }

    if (!job.type) {
      errors.push({
        field: `jobs[${index}].type`,
        message: 'Job must have a type',
        severity: 'error'
      });
    }

    if (job.target && !Array.isArray(job.target)) {
      errors.push({
        field: `jobs[${index}].target`,
        message: 'Job target must be an array',
        severity: 'error'
      });
    }

    if (job.target && Array.isArray(job.target) && job.target.length !== 2) {
      errors.push({
        field: `jobs[${index}].target`,
        message: 'Job target must have exactly 2 coordinates',
        severity: 'error'
      });
    }

    if (job.progress !== undefined && (typeof job.progress !== 'number' || job.progress < 0 || job.progress > 1)) {
      errors.push({
        field: `jobs[${index}].progress`,
        message: 'Job progress must be a number between 0 and 1',
        severity: 'error'
      });
    }

    return errors;
  }

  static compareSnapshots(before: Snapshot, after: Snapshot): ValidationResult {
    const errors: ValidationError[] = [];
    const warnings: ValidationError[] = [];

    // Tick should advance
    if (typeof before.tick === 'number' && typeof after.tick === 'number') {
      if (after.tick <= before.tick) {
        errors.push({
          field: 'tick',
          message: `Tick should advance from ${before.tick} to greater value, got ${after.tick}`,
          severity: 'error'
        });
      }
    }

    // Agent count consistency
    const beforeAgentCount = Array.isArray(before.agents) ? before.agents.length : 0;
    const afterAgentCount = Array.isArray(after.agents) ? after.agents.length : 0;
    
    if (Math.abs(afterAgentCount - beforeAgentCount) > 1) {
      warnings.push({
        field: 'agents',
        message: `Agent count changed significantly: ${beforeAgentCount} -> ${afterAgentCount}`,
        severity: 'warning'
      });
    }

    // Validate agent position changes
    if (Array.isArray(before.agents) && Array.isArray(after.agents)) {
      const beforeAgents = new Map(before.agents.map(a => [a.id, a]));
      const afterAgents = new Map(after.agents.map(a => [a.id, a]));

      afterAgents.forEach((afterAgent, id) => {
        const beforeAgent = beforeAgents.get(id);
        if (beforeAgent && beforeAgent.pos && afterAgent.pos) {
          const distance = this.calculateHexDistance(beforeAgent.pos, afterAgent.pos);
          if (distance > 3) { // Agents shouldn't move more than 3 hexes per tick
            warnings.push({
              field: `agents[${id}].pos`,
              message: `Agent moved ${distance} hexes in one tick, which may be too fast`,
              severity: 'warning'
            });
          }
        }
      });
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings
    };
  }

  private static calculateHexDistance(pos1: [number, number], pos2: [number, number]): number {
    const [q1, r1] = pos1;
    const [q2, r2] = pos2;
    return (Math.abs(q1 - q2) + Math.abs(q1 + r1 - q2 - r2) + Math.abs(r1 - r2)) / 2;
  }

  static validateResourceConservation(before: Snapshot, after: Snapshot): ValidationResult {
    const errors: ValidationError[] = [];
    const warnings: ValidationError[] = [];

    // Count total resources before and after
    const beforeResources = this.countTotalResources(before);
    const afterResources = this.countTotalResources(after);

    Object.entries(beforeResources).forEach(([resource, beforeCount]) => {
      const afterCount = afterResources[resource] || 0;
      const difference = afterCount - beforeCount;

      // Allow small differences due to gathering/consumption
      if (Math.abs(difference) > 5) {
        warnings.push({
          field: 'resources',
          message: `Resource ${resource} changed significantly: ${beforeCount} -> ${afterCount} (${difference > 0 ? '+' : ''}${difference})`,
          severity: 'warning'
        });
      }
    });

    return {
      isValid: errors.length === 0,
      errors,
      warnings
    };
  }

  private static countTotalResources(snapshot: Snapshot): Record<string, number> {
    const resources: Record<string, number> = {};

    // Count resources in tiles
    if (snapshot.tiles) {
      Object.values(snapshot.tiles).forEach(tile => {
        if (tile.resource) {
          resources[tile.resource] = (resources[tile.resource] || 0) + 1;
        }
      });
    }

    // Count resources in stockpiles
    if (snapshot.stockpiles) {
      Object.values(snapshot.stockpiles).forEach(stockpile => {
        const resource = stockpile.resource || stockpile[':resource'];
        const qty = stockpile.currentQty || stockpile['current-qty'] || 0;
        if (resource && typeof qty === 'number') {
          resources[resource] = (resources[resource] || 0) + qty;
        }
      });
    }

    // Count resources in agent inventories
    if (snapshot.agents) {
      snapshot.agents.forEach(agent => {
        if (agent.inventory) {
          Object.entries(agent.inventory).forEach(([resource, qty]) => {
            if (typeof qty === 'number') {
              resources[resource] = (resources[resource] || 0) + qty;
            }
          });
        }
      });
    }

    return resources;
  }
}