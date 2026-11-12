package com.smartstock.service;

import com.smartstock.dto.ProdutoRequest;
import com.smartstock.exception.RecursoNaoEncontradoException;
import com.smartstock.exception.RegraDeNegocioException;
import com.smartstock.model.Produto;
import com.smartstock.repository.EstoqueRepository;
import com.smartstock.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtos;
    private final EstoqueRepository lotes;

    public ProdutoService(ProdutoRepository produtos, EstoqueRepository lotes) {
        this.produtos = produtos;
        this.lotes = lotes;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return produtos.findAll();
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return produtos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
    }

    @Transactional
    public Produto criar(ProdutoRequest req) {
        if (produtos.existsByCodigoBarras(req.codigoBarras())) {
            throw new RegraDeNegocioException("Já existe um produto com este código de barras.");
        }
        return produtos.save(preencher(new Produto(), req));
    }

    @Transactional
    public Produto atualizar(Long id, ProdutoRequest req) {
        Produto produto = buscar(id);
        if (produtos.existsByCodigoBarrasAndIdNot(req.codigoBarras(), id)) {
            throw new RegraDeNegocioException("Já existe outro produto com este código de barras.");
        }
        return produtos.save(preencher(produto, req));
    }

    @Transactional
    public void excluir(Long id) {
        Produto produto = buscar(id);
        if (lotes.existsByProdutoId(id)) {
            throw new RegraDeNegocioException("Produto possui lotes cadastrados e não pode ser excluído.");
        }
        produtos.delete(produto);
    }

    private Produto preencher(Produto produto, ProdutoRequest req) {
        produto.setCodigoBarras(req.codigoBarras());
        produto.setNome(req.nome());
        produto.setCategoria(req.categoria());
        produto.setPrecoOriginal(req.precoOriginal());
        produto.setEstoqueMinimo(req.estoqueMinimo());
        return produto;
    }
}
