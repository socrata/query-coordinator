package com.socrata.querycoordinator.defect

import com.socrata.querycoordinator.QueryRewritingTestUtility.AssertRewriteDefault
import com.socrata.soql.types.{SoQLBoolean, SoQLFloatingTimestamp, SoQLNumber, SoQLText}
import org.scalatest.FunSuite

//https://socrata.atlassian.net/browse/EN-58977
class EN58977 extends FunSuite{

  //This rewrite should fail from a mismatch of groupbys in the left pipe, and (after being fixed) fail on the right pipe because of wrong context, causing a rewrite NOT to happen.
  test("example one, rollup missing groupby in query. Should not rewrite.") {
    AssertRewriteDefault(
      Map(
        "_" -> Map(
          "rowkey_column" -> (SoQLNumber.t, "rowkey"),
          "efspname_column" -> (SoQLText.t, "efspname"),
          "county_column" -> (SoQLText.t, "county"),
          "office_column" -> (SoQLText.t, "office"),
          "orgchartname_column" -> (SoQLText.t, "orgchartname"),
          "nodeid_column" -> (SoQLNumber.t, "nodeid"),
          "casenumber_column" -> (SoQLText.t, "casenumber"),
          "envelopeid_column" -> (SoQLNumber.t, "envelopeid"),
          "isinitial_column" -> (SoQLBoolean.t, "isinitial"),
          "filingid_column" -> (SoQLNumber.t, "filingid"),
          "filingtype_column" -> (SoQLText.t, "filingtype"),
          "submitted_column" -> (SoQLFloatingTimestamp.t, "submitted"),
          "submittedday_column" -> (SoQLText.t, "submittedday"),
          "filingstate_column" -> (SoQLText.t, "filingstate"),
          "casecategorycode_column" -> (SoQLText.t, "casecategorycode"),
          "casetypecode_column" -> (SoQLText.t, "casetypecode"),
          "filingcode_column" -> (SoQLText.t, "filingcode"),
          "isindividualfirm_column" -> (SoQLBoolean.t, "isindividualfirm"),
          "username_column" -> (SoQLText.t, "username"),
          "firmid_column" -> (SoQLNumber.t, "firmid"),
          "firmname_column" -> (SoQLText.t, "firmname"),
          "rejectionreason_column" -> (SoQLText.t, "rejectionreason"),
          "numberofdocuments_column" -> (SoQLNumber.t, "numberofdocuments"),
          "underreview_column" -> (SoQLFloatingTimestamp.t, "underreview"),
          "reviewed_column" -> (SoQLFloatingTimestamp.t, "reviewed"),
          "primaryamount_column" -> (SoQLNumber.t, "primaryamount"),
          "secondaryamount_column" -> (SoQLNumber.t, "secondaryamount"),
          "systempaymentaccounttype_column" -> (SoQLText.t, "systempaymentaccounttype"),
          "adjustedsubmitdate_column" -> (SoQLFloatingTimestamp.t, "adjustedsubmitdate"),
          "clerkresponsetime_column" -> (SoQLNumber.t, "clerkresponsetime"),
          "insertedby_column" -> (SoQLText.t, "insertedby"),
          "clerkname_column" -> (SoQLText.t, "clerkname"),

        )
      ),
      Map(
        "one" -> "SELECT `clerkname` AS `clerkname`, date_trunc_ymd(`submitted`) AS `date_trunc_ymd_submitted`, date_trunc_ym(`submitted`) AS `date_trunc_ym_submitted`, date_trunc_y(`submitted`) AS `date_trunc_y_submitted`, date_extract_woy(`submitted`) AS `date_extract_woy_submitted`, min(`submitted`) AS `min_submitted`, max(`submitted`) AS `max_submitted`, count(*) AS `count`, count(`filingid`) AS `count_filingid`, count(`filingstate`) AS `count_filingstate`, count(`clerkname`) AS `count_clerkname` GROUP BY `clerkname`, `date_trunc_ymd_submitted`, `date_trunc_ym_submitted`, `date_trunc_y_submitted`, `date_extract_woy_submitted`"
      ),
      //The issue is here, the 'count(*)' from the rollup is being rewritten into the right-side pipe
      //Left-side pipe correctly fails to rewrite due to groupby mismatch
      "select count(*) group by casecategorycode |> select count(*) as rows",
      "select count(*) group by casecategorycode |> select count(*) as rows",
      None
    )
  }

  test("example two, with 'casecategorycode' added to the rollup. Should rewrite.") {
    AssertRewriteDefault(
      Map(
        "_" -> Map(
          "rowkey_column" -> (SoQLNumber.t, "rowkey"),
          "efspname_column" -> (SoQLText.t, "efspname"),
          "county_column" -> (SoQLText.t, "county"),
          "office_column" -> (SoQLText.t, "office"),
          "orgchartname_column" -> (SoQLText.t, "orgchartname"),
          "nodeid_column" -> (SoQLNumber.t, "nodeid"),
          "casenumber_column" -> (SoQLText.t, "casenumber"),
          "envelopeid_column" -> (SoQLNumber.t, "envelopeid"),
          "isinitial_column" -> (SoQLBoolean.t, "isinitial"),
          "filingid_column" -> (SoQLNumber.t, "filingid"),
          "filingtype_column" -> (SoQLText.t, "filingtype"),
          "submitted_column" -> (SoQLFloatingTimestamp.t, "submitted"),
          "submittedday_column" -> (SoQLText.t, "submittedday"),
          "filingstate_column" -> (SoQLText.t, "filingstate"),
          "casecategorycode_column" -> (SoQLText.t, "casecategorycode"),
          "casetypecode_column" -> (SoQLText.t, "casetypecode"),
          "filingcode_column" -> (SoQLText.t, "filingcode"),
          "isindividualfirm_column" -> (SoQLBoolean.t, "isindividualfirm"),
          "username_column" -> (SoQLText.t, "username"),
          "firmid_column" -> (SoQLNumber.t, "firmid"),
          "firmname_column" -> (SoQLText.t, "firmname"),
          "rejectionreason_column" -> (SoQLText.t, "rejectionreason"),
          "numberofdocuments_column" -> (SoQLNumber.t, "numberofdocuments"),
          "underreview_column" -> (SoQLFloatingTimestamp.t, "underreview"),
          "reviewed_column" -> (SoQLFloatingTimestamp.t, "reviewed"),
          "primaryamount_column" -> (SoQLNumber.t, "primaryamount"),
          "secondaryamount_column" -> (SoQLNumber.t, "secondaryamount"),
          "systempaymentaccounttype_column" -> (SoQLText.t, "systempaymentaccounttype"),
          "adjustedsubmitdate_column" -> (SoQLFloatingTimestamp.t, "adjustedsubmitdate"),
          "clerkresponsetime_column" -> (SoQLNumber.t, "clerkresponsetime"),
          "insertedby_column" -> (SoQLText.t, "insertedby"),
          "clerkname_column" -> (SoQLText.t, "clerkname"),

        )
      ),
      Map(
        "one" -> "SELECT `casecategorycode`,`clerkname` AS `clerkname`, date_trunc_ymd(`submitted`) AS `date_trunc_ymd_submitted`, date_trunc_ym(`submitted`) AS `date_trunc_ym_submitted`, date_trunc_y(`submitted`) AS `date_trunc_y_submitted`, date_extract_woy(`submitted`) AS `date_extract_woy_submitted`, min(`submitted`) AS `min_submitted`, max(`submitted`) AS `max_submitted`, count(*) AS `count`, count(`filingid`) AS `count_filingid`, count(`filingstate`) AS `count_filingstate`, count(`clerkname`) AS `count_clerkname` GROUP BY `casecategorycode`, `clerkname`, `date_trunc_ymd_submitted`, `date_trunc_ym_submitted`, `date_trunc_y_submitted`, `date_extract_woy_submitted`"
      ),
      "select count(*) group by casecategorycode |> select count(*) as rows",
      "select coalesce(sum(c9),0) as count group by c1 |> select count(*) as rows",
      Some("one")
    )
  }

}
