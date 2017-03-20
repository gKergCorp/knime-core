/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   20.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sag;

import org.apache.commons.math3.util.MathUtils;
import org.knime.base.node.mine.regression.logistic.learner4.glmnet.TrainingRow;

/**
 * WeightVector implementation that uses a scalar variable to implement simple scaling
 * of all weights.
 *
 * @author Adrian Nembach, KNIME.com
 */
class ScaledWeightVector <T extends TrainingRow> extends AbstractWeightVector<T> {
    private double m_scale;

    public ScaledWeightVector(final int nFets, final int nCats) {
        super(nFets, nCats);
        m_scale = 1.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scale(final double alpha, final double lambda) {
        m_scale *= 1 - alpha * lambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final double alpha, final double[][] d, final int nCovered) {
        updateData((final double val, final int c, final int i) -> val - alpha * d[c][i] / (m_scale * nCovered));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkNormalize() {
        if (m_scale > 1e100 || m_scale < -1e100 || (m_scale > 0 && m_scale < 1e-100) || (m_scale < 0 && m_scale > -1e-100)) {
            doFinalize();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[][] getWeightVector() {
        doFinalize();
        return super.getWeightVector();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalize(final double[][] d) {
        doFinalize();
    }

    private void doFinalize() {
        // a scale of 1.0 means that no update is necessary
        if (!MathUtils.equals(m_scale, 1.0)) {
            updateData((final double val, final int c, final int i) -> val * m_scale);
            m_scale = 1.0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] predict(final T row) {
        double[] prediction = super.predict(row);
        for (int c = 0; c < prediction.length; c++) {
            prediction[c] *= m_scale;
        }
        return prediction;
    }
}
