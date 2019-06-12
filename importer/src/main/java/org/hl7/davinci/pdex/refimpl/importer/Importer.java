package org.hl7.davinci.pdex.refimpl.importer;

public interface Importer {

  /**
   *  This is module for demo purpose and it is configured to wotk with Patient/$everything
   *  Target system Configuration is stored in @{{@link TargetConfiguration} class.
   *  Source system configuration is passed as argument in {@link ImportRequest}
   *
   * TODO Our plans are:
   *  -change interface to: Bundle importRecords(Bundle bundle);
   *  -make each implementation responsible for binding resource
   *  (e.g. PatientImporter knows about Coverage.subscriptionId that binds 2 records together)
   *  -system configurations should be specific to importer
   */
  void importRecords(ImportRequest importRequest);

}


