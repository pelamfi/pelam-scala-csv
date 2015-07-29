package fi.pelam.csv

import fi.pelam.csv.cell.Cell

/**
 * The "type" of a cell in a [[Table]] is considered a pair of objects each
 * identifying the row type and column type respectively.
 * <p>
 * This type concept should not be confused with general scala types. Cell, row and column
 * types typically just some suitable (case) objects defined by the client code.
 * <p>
 * Cell types are used in [[TableReader]] to map which actual subtypes of [[fi.pelam.csv.cell.Cell]] should
 * be used for each position in table.
 *
 * @param rowType instance of row type defined by client code.
 * @param colType instance of column type defined by client code.
 * @tparam RT the type of all row types used by the client code.
 * @tparam CT the type of all column types used by the client code.
 */
case class CellType[RT, CT](rowType: RT, colType: CT) {

}
