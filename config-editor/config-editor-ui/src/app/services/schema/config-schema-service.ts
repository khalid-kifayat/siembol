import { UiMetadata } from '@model/ui-metadata-map';
import { cloneDeep } from 'lodash';
import { ConfigData, Config } from '@app/model';
import { JSONSchema7 } from 'json-schema';
import * as omitEmpty from 'omit-empty';
import { SchemaService } from './schema.service';

export class ConfigSchemaService extends SchemaService {
  private readonly _schema: JSONSchema7;

  constructor(protected uiMetadata: UiMetadata, protected user: string, protected originalSchema: JSONSchema7) {
    super(uiMetadata, user, originalSchema);
    if (this.uiMetadata.unionType) {
      this.unionPath = this.uiMetadata.unionType.unionPath ?? undefined;
      this.selectorName = this.uiMetadata.unionType.unionSelectorName ?? undefined;
    }

    //NOTE: we need to modify the schema to handle optionals, unions and remove metadata
    this._schema = this.returnSubTree(this.originalSchema, this.uiMetadata.perConfigSchemaPath) as JSONSchema7;
    this.wrapOptionalsInSchema(this._schema, '', '');
    this.formatTitlesInSchema(this._schema, '');
    delete this._schema.properties[this.uiMetadata.name];
    delete this._schema.properties[this.uiMetadata.author];
    delete this._schema.properties[this.uiMetadata.version];
    this._schema.required = this._schema.required.filter(
      f => f !== this.uiMetadata.name && f !== this.uiMetadata.author && f !== this.uiMetadata.version
    );
  }

  public get schema() {
    return this._schema;
  }

  public createDeploymentSchema(): JSONSchema7 {
    const depSchema = cloneDeep(this.originalSchema);
    depSchema.properties[this.uiMetadata.deployment.config_array] = {};
    delete depSchema.properties[this.uiMetadata.deployment.config_array];
    delete depSchema.properties[this.uiMetadata.deployment.version];
    depSchema.required = depSchema.required.filter(element => {
      if (element !== this.uiMetadata.deployment.version && element !== this.uiMetadata.deployment.config_array) {
        return true;
      }

      return false;
    });

    return depSchema;
  }

  public cleanConfigData(configData: ConfigData): ConfigData {
    let cfg = this.produceOrderedJson(configData, '/');
    cfg = omitEmpty(cfg);
    return cfg;
  }

  public cleanConfig(config: Config): Config {
    config.configData = this.unwrapConfig(config.configData);
    if (config.isNew) {
      config.configData[this.uiMetadata.name] = config.name;
      config.configData[this.uiMetadata.version] = config.version = 0;
      config.configData[this.uiMetadata.author] = config.author = this.user;
    } else {
      config.configData[this.uiMetadata.name] = config.name;
      config.configData[this.uiMetadata.version] = config.version;
      config.configData[this.uiMetadata.author] = config.author;
    }

    config.description = config.configData[this.uiMetadata.description];
    config.configData = this.cleanConfigData(config.configData);
    return config;
  }
}
